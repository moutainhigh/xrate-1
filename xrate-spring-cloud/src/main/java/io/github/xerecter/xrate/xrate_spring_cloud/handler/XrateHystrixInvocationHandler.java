package io.github.xerecter.xrate.xrate_spring_cloud.handler;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import io.github.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import io.github.xerecter.xrate.xrate_core.util.BeanUtil;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import feign.Util;
import feign.hystrix.FallbackFactory;
import feign.hystrix.SetterFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import rx.Completable;
import rx.Observable;
import rx.Single;

@Slf4j
@Data
public class XrateHystrixInvocationHandler implements InvocationHandler {

    private final Target<?> target;
    private final Map<Method, MethodHandler> dispatch;
    // Nullable
    private final FallbackFactory<?> fallbackFactory;
    private final Map<Method, Method> fallbackMethodMap;
    private final Map<Method, Setter> setterMethodMap;
    private IObjectSerializerService objectSerializerService;
    private Class<?> currInterface;

    public XrateHystrixInvocationHandler(
            Target<?> target,
            Map<Method, MethodHandler> dispatch,
            FallbackFactory<?> fallbackFactory,
            Map<Method, Method> fallbackMethodMap,
            Map<Method, Setter> setterMethodMap) {
        this.target = target;
        this.dispatch = dispatch;
        this.fallbackFactory = fallbackFactory;
        this.fallbackMethodMap = fallbackMethodMap;
        this.setterMethodMap = setterMethodMap;
    }

    /**
     * If the method param of InvocationHandler.invoke is not accessible, i.e in a package-private
     * interface, the fallback call in hystrix command will fail cause of access restrictions. But
     * methods in dispatch are copied methods. So setting access to dispatch method doesn't take
     * effect to the method in InvocationHandler.invoke. Use map to store a copy of method to invoke
     * the fallback to bypass this and reducing the count of reflection calls.
     *
     * @return cached methods map for fallback invoking
     */
    static Map<Method, Method> toFallbackMethod(Map<Method, MethodHandler> dispatch) {
        Map<Method, Method> result = new LinkedHashMap<Method, Method>();
        for (Method method : dispatch.keySet()) {
            method.setAccessible(true);
            result.put(method, method);
        }
        return result;
    }

    /**
     * Process all methods in the target so that appropriate setters are created.
     */
    static Map<Method, Setter> toSetters(SetterFactory setterFactory,
                                         Target<?> target,
                                         Set<Method> methods) {
        Map<Method, Setter> result = new LinkedHashMap<Method, Setter>();
        for (Method method : methods) {
            method.setAccessible(true);
            result.put(method, setterFactory.create(target, method));
        }
        return result;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws Throwable {
        // early exit if the invoked method is from java.lang.Object
        // code is the same as ReflectiveFeign.FeignInvocationHandler
        if ("equals".equals(method.getName())) {
            try {
                Object otherHandler =
                        args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
                return equals(otherHandler);
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else if ("hashCode".equals(method.getName())) {
            return hashCode();
        } else if ("toString".equals(method.getName())) {
            return toString();
        }
        HystrixCommand<Object> hystrixCommand = null;
        AtomicInteger recordPosition = new AtomicInteger(-1);
        XrateTransaction xrateTransaction = method.getAnnotation(XrateTransaction.class);
        if (CommonConstants.INIT_START_SIDE != TransactionUtil.getIsStartSide() &&
                xrateTransaction != null) {
            TransactionUtil.printDebugInfo(() -> log.info("method is marked XrateTransaction"));
            IObjectSerializerService objectSerializerService = getObjectSerializerService();
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            int isStartSide = TransactionUtil.getIsStartSide();

            TransactionMember baesCurrTransMb = new TransactionMember();
            TransactionUtil.setCurrTransMb(baesCurrTransMb);
            recordPosition.set(TransactionUtil.getCurrMbPosition());
            XrateConfig currXrateConfig = TransactionUtil.getCurrXrateConfig();
            XrateConfig currConnXrateConfig = TransactionUtil.getCurrConnXrateConfig();

            hystrixCommand = new HystrixCommand<>(setterMethodMap.get(method)) {
                @Override
                protected Object run() throws Exception {
                    try {
                        TransactionUtil.setCurrXrateConfig(currXrateConfig);
                        TransactionUtil.setCurrConnXrateConfig(currConnXrateConfig);
                        TransactionUtil.setCurrTransactionInfo(currTransactionInfo);
                        TransactionUtil.setIsStartSide(isStartSide);
                        TransactionUtil.setCurrMbPosition(recordPosition.get());
                        TransactionUtil.setCurrTransMb(baesCurrTransMb);
                        if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
                            TransactionUtil.printDebugInfo(() -> log.info("exe xrate init trans -> " + currTransactionInfo.getTransId()));
                            TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
                            if (currTransMb == null) {
                                currTransMb = new TransactionMember();
                                TransactionUtil.setCurrTransMb(currTransMb);
                            }
                            List<String> paramClassNames = new ArrayList<>(args.length);
                            for (int i = 0; i < args.length; i++) {
                                paramClassNames.add(args[i].getClass().getName());
                            }
                            currTransMb.setParams(objectSerializerService.serializerObject(args));
                            currTransMb.setParamClassNames(paramClassNames);
                            currTransMb.setMemberClassName(currInterface.getName());
                            currTransMb.setTryName(method.getName());
                            Object result = XrateHystrixInvocationHandler.this.dispatch.get(method).invoke(args);
                            recordPosition.set(TransactionUtil.getCurrMbPosition());
                            TransactionUtil.printDebugInfo(() -> log.info(" remote execute success "));
                            return result;
                        }
                        return XrateHystrixInvocationHandler.this.dispatch.get(method).invoke(args);
                    } catch (Exception e) {
                        throw e;
                    } catch (Throwable t) {
                        throw (Error) t;
                    } finally {
                        TransactionUtil.removeAll();
                    }
                }

                @Override
                protected Object getFallback() {
                    if (fallbackFactory == null) {
                        return super.getFallback();
                    }
                    try {
                        Object fallback = fallbackFactory.create(getExecutionException());
                        Object result = fallbackMethodMap.get(method).invoke(fallback, args);
                        if (isReturnsHystrixCommand(method)) {
                            return ((HystrixCommand) result).execute();
                        } else if (isReturnsObservable(method)) {
                            // Create a cold Observable
                            return ((Observable) result).toBlocking().first();
                        } else if (isReturnsSingle(method)) {
                            // Create a cold Observable as a Single
                            return ((Single) result).toObservable().toBlocking().first();
                        } else if (isReturnsCompletable(method)) {
                            ((Completable) result).await();
                            return null;
                        } else if (isReturnsCompletableFuture(method)) {
                            return ((Future) result).get();
                        } else {
                            return result;
                        }
                    } catch (IllegalAccessException e) {
                        // shouldn't happen as method is public due to being an interface
                        throw new AssertionError(e);
                    } catch (InvocationTargetException | ExecutionException e) {
                        // Exceptions on fallback are tossed by Hystrix
                        throw new AssertionError(e.getCause());
                    } catch (InterruptedException e) {
                        // Exceptions on fallback are tossed by Hystrix
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e.getCause());
                    }
                }
            };
        } else {
            TransactionUtil.removeIsStartSide();
            hystrixCommand = new HystrixCommand<>(setterMethodMap.get(method)) {
                @Override
                protected Object run() throws Exception {
                    try {
                        return XrateHystrixInvocationHandler.this.dispatch.get(method).invoke(args);
                    } catch (Exception e) {
                        throw e;
                    } catch (Throwable t) {
                        throw (Error) t;
                    }
                }

                @Override
                protected Object getFallback() {
                    if (fallbackFactory == null) {
                        return super.getFallback();
                    }
                    try {
                        Object fallback = fallbackFactory.create(getExecutionException());
                        Object result = fallbackMethodMap.get(method).invoke(fallback, args);
                        if (isReturnsHystrixCommand(method)) {
                            return ((HystrixCommand) result).execute();
                        } else if (isReturnsObservable(method)) {
                            // Create a cold Observable
                            return ((Observable) result).toBlocking().first();
                        } else if (isReturnsSingle(method)) {
                            // Create a cold Observable as a Single
                            return ((Single) result).toObservable().toBlocking().first();
                        } else if (isReturnsCompletable(method)) {
                            ((Completable) result).await();
                            return null;
                        } else if (isReturnsCompletableFuture(method)) {
                            return ((Future) result).get();
                        } else {
                            return result;
                        }
                    } catch (IllegalAccessException e) {
                        // shouldn't happen as method is public due to being an interface
                        throw new AssertionError(e);
                    } catch (InvocationTargetException | ExecutionException e) {
                        // Exceptions on fallback are tossed by Hystrix
                        throw new AssertionError(e.getCause());
                    } catch (InterruptedException e) {
                        // Exceptions on fallback are tossed by Hystrix
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e.getCause());
                    }
                }
            };
        }
        if (Util.isDefault(method)) {
            return hystrixCommand.execute();
        } else if (isReturnsHystrixCommand(method)) {
            return hystrixCommand;
        } else if (isReturnsObservable(method)) {
            // Create a cold Observable
            return hystrixCommand.toObservable();
        } else if (isReturnsSingle(method)) {
            // Create a cold Observable as a Single
            return hystrixCommand.toObservable().toSingle();
        } else if (isReturnsCompletable(method)) {
            return hystrixCommand.toObservable().toCompletable();
        } else if (isReturnsCompletableFuture(method)) {
            return new XrateObservableCompletableFuture<>(hystrixCommand);
        }
        Object result = hystrixCommand.execute();
        if (recordPosition.get() > 0) {
            TransactionUtil.setCurrMbPosition(recordPosition.get());
            TransactionUtil.printDebugInfo(() -> log.info(" execute success "));
        }
        return result;
    }

    private boolean isReturnsCompletable(Method method) {
        return Completable.class.isAssignableFrom(method.getReturnType());
    }

    private boolean isReturnsHystrixCommand(Method method) {
        return HystrixCommand.class.isAssignableFrom(method.getReturnType());
    }

    private boolean isReturnsObservable(Method method) {
        return Observable.class.isAssignableFrom(method.getReturnType());
    }

    private boolean isReturnsCompletableFuture(Method method) {
        return CompletableFuture.class.isAssignableFrom(method.getReturnType());
    }

    private boolean isReturnsSingle(Method method) {
        return Single.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XrateHystrixInvocationHandler) {
            XrateHystrixInvocationHandler other = (XrateHystrixInvocationHandler) obj;
            return target.equals(other.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return target.toString();
    }

    private IObjectSerializerService getObjectSerializerService() {
        if (objectSerializerService != null) {
            return objectSerializerService;
        }
        synchronized (this) {
            if (objectSerializerService == null) {
                objectSerializerService = BeanUtil.getSpringCtx().getBean(IObjectSerializerService.class);
            }
        }
        return objectSerializerService;
    }

}
