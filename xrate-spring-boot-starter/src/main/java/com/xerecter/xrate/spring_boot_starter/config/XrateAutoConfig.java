package com.xerecter.xrate.spring_boot_starter.config;


import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.factory.XrateConfigFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

import java.lang.reflect.Field;

@Configuration
@ComponentScans(value = {
        @ComponentScan(basePackages = "com.xerecter.xrate.xrate_core"),
        @ComponentScan(basePackages = "com.xerecter.xrate.xrate_spring_cloud"),
        @ComponentScan(basePackages = "com.xerecter.xrate.xrate_dubbo"),
})
@EnableConfigurationProperties(value = XrateProperties.class)
public class XrateAutoConfig {

    @Bean(name = "xrateConfig")
    @ConditionalOnMissingBean
    public XrateConfig getXrateConfig(XrateProperties xrateProperties, ApplicationContext applicationContext) throws IllegalAccessException {
        XrateConfig xrateConfig = xrateProperties.getConfig();
        if (CommonConstants.MONGODB_PERSISTENCE_WAY.equals(xrateConfig.getPersistenceWay())) {
            xrateConfig.setPersistenceConfig(xrateProperties.getMongodbConfig());
        } else if (CommonConstants.MYSQL_PERSISTENCE_WAY.equals(xrateConfig.getPersistenceWay())) {
            xrateConfig.setPersistenceConfig(xrateProperties.getMySQLConfig());
        }
        return xrateConfig;
    }

}
