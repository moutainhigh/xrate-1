package io.github.xerecter.xrate.xrate_core.factory;

import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.MongodbConfig;
import io.github.xerecter.xrate.xrate_core.entity.MySQLConfig;
import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import io.github.xerecter.xrate.xrate_core.service.impl.MongodbTransactionInfoServiceImpl;
import io.github.xerecter.xrate.xrate_core.service.impl.MySQLTransactionInfoServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.util.Assert;

public class TransactionInfoServiceFactory {

    public static AbstractBeanDefinition getITransactionInfoServiceDefinition(XrateConfig xrateConfig, Object persistenceConfig) {
        if (CommonConstants.MONGODB_PERSISTENCE_WAY.equals(xrateConfig.getPersistenceWay())) {
            Assert.isTrue(StringUtils.isNotBlank(xrateConfig.getPersistenceWay()), "persistence way is null");
            Assert.isTrue(persistenceConfig instanceof MongodbConfig, "mongodb config is error");
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MongodbTransactionInfoServiceImpl.class);
            beanDefinitionBuilder.addConstructorArgValue(xrateConfig);
            beanDefinitionBuilder.addConstructorArgValue(persistenceConfig);
            return beanDefinitionBuilder.getBeanDefinition();
        } else if (CommonConstants.MYSQL_PERSISTENCE_WAY.equals(xrateConfig.getPersistenceWay())) {
            Assert.isTrue(StringUtils.isNotBlank(xrateConfig.getPersistenceWay()), "persistence way is null");
            Assert.isTrue(persistenceConfig instanceof MySQLConfig, "mysql config is error");
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MySQLTransactionInfoServiceImpl.class);
            beanDefinitionBuilder.addConstructorArgValue(xrateConfig);
            beanDefinitionBuilder.addConstructorArgValue(persistenceConfig);
            return beanDefinitionBuilder.getBeanDefinition();
        } else {
            return null;
        }
    }

}
