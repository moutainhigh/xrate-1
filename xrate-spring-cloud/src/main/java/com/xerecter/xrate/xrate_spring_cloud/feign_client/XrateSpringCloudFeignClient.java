package com.xerecter.xrate.xrate_spring_cloud.feign_client;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.util.CommonUtil;
import com.xerecter.xrate.xrate_core.util.ReflectUtil;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import com.xerecter.xrate.xrate_spring_cloud.factory.XrateCachingSpringLoadBalancerFactory;
import feign.Client;
import feign.Request;
import feign.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Data
public class XrateSpringCloudFeignClient extends LoadBalancerFeignClient {

    public XrateSpringCloudFeignClient(Client delegate, CachingSpringLoadBalancerFactory lbClientFactory, SpringClientFactory clientFactory) {
        super(delegate, lbClientFactory, clientFactory);
        try {
            SpringClientFactory factory = ReflectUtil.<SpringClientFactory>getFieldValue("factory", lbClientFactory);
            LoadBalancedRetryFactory loadBalancedRetryFactory = ReflectUtil.<LoadBalancedRetryFactory>getFieldValue("loadBalancedRetryFactory", lbClientFactory);
            Map<String, FeignLoadBalancer> cache = ReflectUtil.<Map<String, FeignLoadBalancer>>getFieldValue("cache", lbClientFactory);
            XrateCachingSpringLoadBalancerFactory xrateCachingSpringLoadBalancerFactory = new XrateCachingSpringLoadBalancerFactory(factory, loadBalancedRetryFactory, cache);
            ReflectUtil.setFieldValue(this, xrateCachingSpringLoadBalancerFactory, "lbClientFactory");
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        Response executeResult = super.execute(request, options);
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        if (currTransactionInfo != null) {
            if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
                List<String> keys = executeResult.headers().keySet().stream().filter(key ->
                        key.toLowerCase().startsWith(CommonConstants.TRANS_POSITION_KEY))
                        .sorted()
                        .collect(Collectors.toList());
                int position = Integer.parseInt(executeResult.headers().get(keys.get(keys.size() - 1)).stream().findFirst().get());
                TransactionUtil.printDebugInfo(() -> log.info("result position -> " + position));
                TransactionUtil.setCurrMbPosition(position);
            }
        }
        return executeResult;
    }

//    @Override
//    protected IOException findIOException(Throwable t) {
//        try {
//            Method method = loadBalancerFeignClient.getClass().getDeclaredMethod("findIOException");
//            method.setAccessible(true);
//            return (IOException) method.invoke(loadBalancerFeignClient, t);
//        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//            e.printStackTrace();
//        }
//        return super.findIOException(t);
//    }
//
//    @Override
//    public Client getDelegate() {
//        return loadBalancerFeignClient.getDelegate();
//    }

}
