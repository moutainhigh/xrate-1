package com.xerecter.xrate_dubbo_demo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import com.xerecter.xrate_dubbo_demo.entity.User;
import com.xerecter.xrate_dubbo_demo.user_service.UserService;
import com.xerecter.xrate_dubbo_demo.user.mapper.UserMapper;
import com.xerecter.xrate_dubbo_demo.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
@Service("userService")
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService, UserService {

    @Override
    @XrateTransaction("minusUserBalanceCancel")
    public boolean minusUserBalance(Long userId, Double amount) {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId);
        updateWrapper.setSql("user_balance = user_balance - " + amount);
        log.info("userId -> " + userId + " amount ->" + amount);
        int updateRows = this.baseMapper.update(null, updateWrapper);
        log.info("result -> " + (updateRows > 0));
        return updateRows > 0;
    }

    public void minusUserBalanceCancel(Long userId, Double amount) {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId);
        updateWrapper.setSql("user_balance = user_balance + " + amount);
        this.baseMapper.update(null, updateWrapper);
        log.info("minusUserBalanceCancel -> ");
    }

}
