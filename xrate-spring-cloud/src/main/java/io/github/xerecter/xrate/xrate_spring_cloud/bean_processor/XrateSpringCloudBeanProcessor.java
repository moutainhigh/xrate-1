package io.github.xerecter.xrate.xrate_spring_cloud.bean_processor;

import com.netflix.hystrix.HystrixCommand;
import io.github.xerecter.xrate.xrate_core.util.ReflectUtil;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import io.github.xerecter.xrate.xrate_spring_cloud.feign_client.XrateSpringCloudFeignClient;
import io.github.xerecter.xrate.xrate_spring_cloud.handler.XrateDefaultInvocationHandler;
import io.github.xerecter.xrate.xrate_spring_cloud.handler.XrateFeignInvocationHandler;
import io.github.xerecter.xrate.xrate_spring_cloud.handler.XrateHystrixInvocationHandler;
import feign.Client;
import feign.InvocationHandlerFactory;
import feign.Target;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

@Component
@Slf4j
public class XrateSpringCloudBeanProcessor implements BeanPostProcessor {

    /**
     * feign handler类名
     */
    private static String FEIGN_HANDLER = "feign.ReflectiveFeign$FeignInvocationHandler";

    /**
     * hystrix handler类名
     */
    private static String HYSTRIX_HANDLER = "feign.hystrix.HystrixInvocationHandler";

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        FeignClient feignClient = (FeignClient) ReflectUtil.<FeignClient>getAnnotationByInterface(bean.getClass(), FeignClient.class);
        if (feignClient != null) {
            if (Proxy.isProxyClass(bean.getClass())) {
                try {
                    InvocationHandler invocationHandler = Proxy.getInvocationHandler(bean);
                    String className = invocationHandler.getClass().getName();
                    if (FEIGN_HANDLER.equals(className)) {
                        TransactionUtil.printDebugInfo(() -> log.info("spring cloud feign invocation -> "));
                        Target<?> target = ReflectUtil.<Target<?>>getFieldValue("target", invocationHandler);
                        Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = ReflectUtil.<Map<Method, InvocationHandlerFactory.MethodHandler>>getFieldValue("dispatch", invocationHandler);
                        XrateFeignInvocationHandler xrateFeignInvocationHandler = new XrateFeignInvocationHandler(target, dispatch);
                        xrateFeignInvocationHandler.setCurrInterface(bean.getClass().getInterfaces()[0]);
                        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), bean.getClass().getInterfaces(), xrateFeignInvocationHandler);
                    } else if (HYSTRIX_HANDLER.equals(className)) {
                        TransactionUtil.printDebugInfo(() -> log.info("spring cloud feign invocation -> "));
                        Target<?> target = ReflectUtil.<Target<?>>getFieldValue("target", invocationHandler);
                        Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = ReflectUtil.<Map<Method, InvocationHandlerFactory.MethodHandler>>getFieldValue("dispatch", invocationHandler);
                        FallbackFactory<?> fallbackFactory = ReflectUtil.<FallbackFactory<?>>getFieldValue("fallbackFactory", invocationHandler);
                        Map<Method, Method> fallbackMethodMap = ReflectUtil.<Map<Method, Method>>getFieldValue("fallbackMethodMap", invocationHandler);
                        Map<Method, HystrixCommand.Setter> setterMethodMap = ReflectUtil.<Map<Method, HystrixCommand.Setter>>getFieldValue("setterMethodMap", invocationHandler);
                        XrateHystrixInvocationHandler xrateHystrixInvocationHandler = new XrateHystrixInvocationHandler(target, dispatch, fallbackFactory, fallbackMethodMap, setterMethodMap);
                        xrateHystrixInvocationHandler.setCurrInterface(bean.getClass().getInterfaces()[0]);
                        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), bean.getClass().getInterfaces(), xrateHystrixInvocationHandler);
                    } else {
                        TransactionUtil.printDebugInfo(() -> log.info("spring cloud feign invocation -> "));
                        XrateDefaultInvocationHandler xrateDefaultInvocationHandler = new XrateDefaultInvocationHandler();
                        xrateDefaultInvocationHandler.setDelegate(invocationHandler);
                        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), bean.getClass().getInterfaces(), xrateDefaultInvocationHandler);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (bean instanceof LoadBalancerFeignClient) {
            LoadBalancerFeignClient client = (LoadBalancerFeignClient) bean;
            Client delegate = null;
            CachingSpringLoadBalancerFactory lbClientFactory = null;
            SpringClientFactory clientFactory = null;
            try {
                Field delegateField = client.getClass().getDeclaredField("delegate");
                delegateField.setAccessible(true);
                Field lbClientFactoryField = client.getClass().getDeclaredField("lbClientFactory");
                lbClientFactoryField.setAccessible(true);
                Field clientFactoryField = client.getClass().getDeclaredField("clientFactory");
                clientFactoryField.setAccessible(true);
                delegate = (Client) delegateField.get(client);
                lbClientFactory = (CachingSpringLoadBalancerFactory) lbClientFactoryField.get(client);
                clientFactory = (SpringClientFactory) clientFactoryField.get(client);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return new XrateSpringCloudFeignClient(delegate, lbClientFactory, clientFactory);
        }
        return bean;
    }

}
