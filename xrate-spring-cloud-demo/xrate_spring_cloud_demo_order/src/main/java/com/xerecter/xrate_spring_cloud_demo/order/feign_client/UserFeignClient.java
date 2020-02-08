package com.xerecter.xrate_spring_cloud_demo.order.feign_client;

import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "user", fallback = UserFeignClientHystrix.class, path = "/user/user")
public interface UserFeignClient {

    @PostMapping("/minusUserBalance")
    @XrateTransaction("minusUserBalanceCancel")
    public Boolean minusUserBalance(
            @RequestParam("userId") Long userId,
            @RequestParam("amounts") Double amounts
    );

}
