package io.github.xerecter.xrate.xrate_spring_cloud.handler;

import io.github.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import io.github.xerecter.xrate.xrate_core.util.BeanUtil;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static feign.Util.checkNotNull;

@Slf4j
@Data
public class XrateFeignInvocationHandler implements InvocationHandler {

    private final Target target;
    private final Map<Method, MethodHandler> dispatch;
    private IObjectSerializerService objectSerializerService;
    private Class<?> currInterface;

    public XrateFeignInvocationHandler(Target target, Map<Method, MethodHandler> dispatch) {
        this.target = checkNotNull(target, "target");
        this.dispatch = checkNotNull(dispatch, "dispatch for %s", target);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        XrateTransaction xrateTransaction = method.getAnnotation(XrateTransaction.class);
        if (CommonConstants.INIT_START_SIDE != TransactionUtil.getIsStartSide() &&
                xrateTransaction != null) {
            TransactionUtil.printDebugInfo(() -> log.info("method is marked XrateTransaction"));
            IObjectSerializerService objectSerializerService = getObjectSerializerService();
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
                TransactionUtil.printDebugInfo(() -> log.info("exe xrate init trans -> " + currTransactionInfo.getTransId()));
                TransactionMember currTransMb = new TransactionMember();
                TransactionUtil.setCurrTransMb(currTransMb);
                List<String> paramClassNames = new ArrayList<>(args.length);
                for (int i = 0; i < args.length; i++) {
                    paramClassNames.add(args[i].getClass().getName());
                }
                currTransMb.setParams(objectSerializerService.serializerObject(args));
                currTransMb.setParamClassNames(paramClassNames);
                currTransMb.setMemberClassName(currInterface.getName());
                currTransMb.setTryName(method.getName());
                Object result = dispatch.get(method).invoke(args);
                TransactionUtil.printDebugInfo(() -> log.info(" remote execute success "));
                return result;
            }
            return dispatch.get(method).invoke(args);
        } else {
            TransactionUtil.removeIsStartSide();
            return dispatch.get(method).invoke(args);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XrateFeignInvocationHandler) {
            XrateFeignInvocationHandler other = (XrateFeignInvocationHandler) obj;
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
