package com.xerecter.xrate_spring_cloud_demo.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xerecter.xrate_spring_cloud_demo.entity.OrderInfo;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
public interface IOrderInfoService extends IService<OrderInfo> {

    public boolean addOrder(OrderInfo order);

}
