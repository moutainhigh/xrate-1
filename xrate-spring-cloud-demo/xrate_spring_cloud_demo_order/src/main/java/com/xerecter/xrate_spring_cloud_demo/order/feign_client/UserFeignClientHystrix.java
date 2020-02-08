package com.xerecter.xrate_spring_cloud_demo.order.feign_client;

import org.springframework.stereotype.Component;

@Component
public class UserFeignClientHystrix implements UserFeignClient {
    @Override
    public Boolean minusUserBalance(Long userId, Double amounts) {
        return null;
    }
}
