package com.xerecter.xrate_spring_cloud_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
public class OrderInfo implements Serializable {

private static final long serialVersionUID=1L;

    @TableId(value = "order_id", type = IdType.AUTO)
    private long orderId;

    @TableField("order_amount")
    private double orderAmount;

    @TableField("order_create_date")
    private LocalDateTime orderCreateDate;

    @TableField("user_id")
    private long userId;

    @TableField("merchant_id")
    private long merchantId;


    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public double getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(double orderAmount) {
        this.orderAmount = orderAmount;
    }

    public LocalDateTime getOrderCreateDate() {
        return orderCreateDate;
    }

    public void setOrderCreateDate(LocalDateTime orderCreateDate) {
        this.orderCreateDate = orderCreateDate;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    @Override
    public String toString() {
        return "OrderInfo{" +
        "orderId=" + orderId +
        ", orderAmount=" + orderAmount +
        ", orderCreateDate=" + orderCreateDate +
        ", userId=" + userId +
        ", merchantId=" + merchantId +
        "}";
    }
}
