package com.xerecter.xrate_spring_cloud_demo.merchant.controller;


import com.xerecter.xrate_spring_cloud_demo.merchant.service.IMerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
@RestController
@RequestMapping("/merchant")
public class MerchantController {

    @Autowired
    IMerchantService merchantService;

    @PostMapping("/plusMerchantBalance")
    public Boolean plusMerchantBalance(
            @RequestParam(value = "merchantId", required = false) Long merchantId,
            @RequestParam(value = "amount", required = false) Double amount
    ) {
        return merchantService.plusMerchantBalance(merchantId, amount);
    }

}

