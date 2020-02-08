package com.xerecter.xrate_spring_cloud_demo.order.controller;


import com.xerecter.xrate.xrate_core.util.CommonUtil;
import com.xerecter.xrate_spring_cloud_demo.entity.OrderInfo;
import com.xerecter.xrate_spring_cloud_demo.order.service.IOrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
@RestController
@RequestMapping("/order")
public class OrderInfoController {

    @Autowired
    IOrderInfoService orderService;

    @PostMapping("/addOrder")
    public String addOrder(
            @RequestParam("orderId") Long orderId,
            @RequestParam("orderAmount") Double orderAmount,
            @RequestParam("userId") Long userId,
            @RequestParam("merchantId") Long merchantId
    ) {
        OrderInfo order = new OrderInfo();
        order.setOrderId(orderId);
        order.setMerchantId(merchantId);
        order.setUserId(userId);
        order.setOrderAmount(orderAmount);
        order.setOrderCreateDate(LocalDateTime.now());
        boolean addOrder = orderService.addOrder(order);
        return CommonUtil.returnMultiResult(1, addOrder);
    }

}

