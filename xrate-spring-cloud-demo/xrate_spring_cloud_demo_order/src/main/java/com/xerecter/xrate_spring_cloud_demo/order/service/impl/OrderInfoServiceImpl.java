package com.xerecter.xrate_spring_cloud_demo.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import com.xerecter.xrate_spring_cloud_demo.entity.OrderInfo;
import com.xerecter.xrate_spring_cloud_demo.order.feign_client.MerchantFeignClient;
import com.xerecter.xrate_spring_cloud_demo.order.feign_client.UserFeignClient;
import com.xerecter.xrate_spring_cloud_demo.order.mapper.OrderInfoMapper;
import com.xerecter.xrate_spring_cloud_demo.order.service.IOrderInfoService;
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
    UserFeignClient userFeignClient;

    @Autowired
    MerchantFeignClient merchantFeignClient;

    @Override
    @XrateTransaction("addOrderCancel")
    public boolean addOrder(OrderInfo order) {
        int insertRows = this.baseMapper.insert(order);
        Assert.isTrue(insertRows > 0, "add order error");
        Assert.isTrue(userFeignClient.minusUserBalance(order.getUserId(), order.getOrderAmount()), "user service error");
        Assert.isTrue(merchantFeignClient.plusMerchantBalance(order.getMerchantId(), order.getOrderAmount()), "merchant service error");
        return true;
    }

    public void addOrderCancel(OrderInfo order) {
        this.baseMapper.deleteById(order.getOrderId());
    }

}
