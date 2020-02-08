package com.xerecter.xrate_dubbo_demo.user_service;

import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;

public interface UserService {

    @XrateTransaction("minusUserBalanceCancel")
    public boolean minusUserBalance(Long userId, Double amount);

}
