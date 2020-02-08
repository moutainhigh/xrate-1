package com.xerecter.xrate_spring_cloud_demo.order.feign_client;

import org.springframework.stereotype.Component;

@Component
public class MerchantFeignClientHystrix implements MerchantFeignClient {
    @Override
    public Boolean plusMerchantBalance(Long merchantId, Double amount) {
        return null;
    }
}
