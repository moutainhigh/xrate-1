package com.xerecter.xrate_dubbo_demo.merchant_service;

import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;

public interface MerchantService {

    @XrateTransaction("plusMerchantBalanceCancel")
    public boolean plusMerchantBalance(Long merchantId, Double amount);

}
