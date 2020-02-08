package com.xerecter.xrate.xrate_core.factory;

import com.xerecter.xrate.xrate_core.entity.MongodbConfig;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.service.impl.MongodbTransactionInfoServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.util.Assert;

public class TransactionInfoServiceFactory {

    public static AbstractBeanDefinition getITransactionInfoServiceDefinition(XrateConfig xrateConfig, Object persistenceConfig) {
        Assert.isTrue(StringUtils.isNotBlank(xrateConfig.getPersistenceWay()), "persistence way is null");
        Assert.isTrue(persistenceConfig instanceof MongodbConfig, "mongodb config is error");
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MongodbTransactionInfoServiceImpl.class);
        beanDefinitionBuilder.addConstructorArgValue(xrateConfig);
        beanDefinitionBuilder.addConstructorArgValue(persistenceConfig);
        return beanDefinitionBuilder.getBeanDefinition();
    }

}
