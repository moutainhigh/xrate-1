package com.xerecter.xrate_spring_cloud_demo.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import com.xerecter.xrate_spring_cloud_demo.entity.Merchant;
import com.xerecter.xrate_spring_cloud_demo.merchant.mapper.MerchantMapper;
import com.xerecter.xrate_spring_cloud_demo.merchant.service.IMerchantService;
import lombok.extern.slf4j.Slf4j;
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
@Service("merchantService")
@Slf4j
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements IMerchantService {

    @Override
    @XrateTransaction("plusMerchantBalanceCancel")
    public boolean plusMerchantBalance(Long merchantId, Double amount) {
        Assert.isTrue(merchantId != null && amount != null, "info error");
        UpdateWrapper<Merchant> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("merchant_id", merchantId);
        updateWrapper.setSql("merchant_balance = merchant_balance + " + amount);
        log.info("merchantId -> " + merchantId + " amount ->" + amount);
        int updateRows = this.baseMapper.update(null, updateWrapper);
        log.info("result -> " + (updateRows > 0));
//        throw new RuntimeException(" plusMerchantBalance -> ");
        return updateRows > 0;
    }

    public void plusMerchantBalanceCancel(Long merchantId, Double amount) {
        Assert.isTrue(merchantId != null && amount != null, "info error");
        UpdateWrapper<Merchant> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("merchant_id", merchantId);
        updateWrapper.setSql("merchant_balance = merchant_balance - " + amount);
        this.baseMapper.update(null, updateWrapper);
        log.info("plusMerchantBalanceCancel -> ");
    }

}
