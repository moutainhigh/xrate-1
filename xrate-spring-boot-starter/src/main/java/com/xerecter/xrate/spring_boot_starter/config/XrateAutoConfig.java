package com.xerecter.xrate.spring_boot_starter.config;


import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

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
    public XrateConfig getXrateConfig(XrateProperties xrateProperties) {
        XrateConfig xrateConfig = xrateProperties.getConfig();
        if (CommonConstants.MONGODB_PERSISTENCE_WAY.equals(xrateConfig.getPersistenceWay())) {
            xrateConfig.setPersistenceConfig(xrateProperties.getMongodbConfig());
        }
        XrateConfig.setXrateConfigInstance(xrateConfig);
        return xrateConfig;
    }

}
