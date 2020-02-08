package com.xerecter.xrate_dubbo_demo.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import com.xerecter.xrate_dubbo_demo.entity.OrderInfo;
import com.xerecter.xrate_dubbo_demo.merchant_service.MerchantService;
import com.xerecter.xrate_dubbo_demo.order.mapper.OrderInfoMapper;
import com.xerecter.xrate_dubbo_demo.order.service.IOrderInfoService;
import com.xerecter.xrate_dubbo_demo.user_service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService {

    @Autowired
    @Qualifier("userService")
    UserService userService;

    @Autowired
    @Qualifier("merchantService")
    MerchantService merchantService;

    @Override
    @XrateTransaction("addOrderCancel")
    public boolean addOrder(OrderInfo order) {
        int insertRows = this.baseMapper.insert(order);
        Assert.isTrue(insertRows > 0, "add order error");
        Assert.isTrue(userService.minusUserBalance(order.getUserId(), order.getOrderAmount()), "user service error");
        Assert.isTrue(merchantService.plusMerchantBalance(order.getMerchantId(), order.getOrderAmount()), "merchant service error");
        return true;
    }

    public void addOrderCancel(OrderInfo order) {
        this.baseMapper.deleteById(order.getOrderId());
    }

}
