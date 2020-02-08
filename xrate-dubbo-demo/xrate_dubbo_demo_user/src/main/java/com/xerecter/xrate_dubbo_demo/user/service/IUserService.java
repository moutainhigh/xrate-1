package com.xerecter.xrate_dubbo_demo.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xerecter.xrate_dubbo_demo.entity.User;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
public interface IUserService extends IService<User> {

    public boolean minusUserBalance(Long userId, Double amount);

}
