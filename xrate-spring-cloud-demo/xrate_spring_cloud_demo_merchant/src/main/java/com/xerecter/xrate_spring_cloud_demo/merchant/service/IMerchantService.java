package com.xerecter.xrate_spring_cloud_demo.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xerecter.xrate_spring_cloud_demo.entity.Merchant;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author xdd
 * @since 2019-11-09
 */
public interface IMerchantService extends IService<Merchant> {

    public boolean plusMerchantBalance(Long merchantId, Double amount);

}
