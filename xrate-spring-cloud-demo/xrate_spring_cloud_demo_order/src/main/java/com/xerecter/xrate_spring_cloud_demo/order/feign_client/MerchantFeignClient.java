package com.xerecter.xrate_spring_cloud_demo.order.feign_client;

import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "merchant", fallback = MerchantFeignClientHystrix.class, path = "/merchant/merchant")
public interface MerchantFeignClient {

    @PostMapping("/plusMerchantBalance")
    @XrateTransaction("plusMerchantBalanceCancel")
    public Boolean plusMerchantBalance(
            @RequestParam("merchantId") Long merchantId,
            @RequestParam("amount") Double amount
    );

}
