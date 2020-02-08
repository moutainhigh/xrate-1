package com.xerecter.xrate_dubbo_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
public class Merchant implements Serializable {

private static final long serialVersionUID=1L;

    @TableId(value = "merchant_id", type = IdType.AUTO)
    private long merchantId;

    @TableField("merchant_name")
    private String merchantName;

    @TableField("merchant_balance")
    private double merchantBalance;


    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public double getMerchantBalance() {
        return merchantBalance;
    }

    public void setMerchantBalance(double merchantBalance) {
        this.merchantBalance = merchantBalance;
    }

    @Override
    public String toString() {
        return "Merchant{" +
        "merchantId=" + merchantId +
        ", merchantName=" + merchantName +
        ", merchantBalance=" + merchantBalance +
        "}";
    }
}
