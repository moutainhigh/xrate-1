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
public class User implements Serializable {

private static final long serialVersionUID=1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    private long userId;

    @TableField("user_name")
    private String userName;

    @TableField("user_balance")
    private double userBalance;


    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getUserBalance() {
        return userBalance;
    }

    public void setUserBalance(double userBalance) {
        this.userBalance = userBalance;
    }

    @Override
    public String toString() {
        return "User{" +
        "userId=" + userId +
        ", userName=" + userName +
        ", userBalance=" + userBalance +
        "}";
    }
}
