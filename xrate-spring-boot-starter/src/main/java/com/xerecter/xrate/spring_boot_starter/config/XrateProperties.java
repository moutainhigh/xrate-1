package com.xerecter.xrate.spring_boot_starter.config;

import com.xerecter.xrate.xrate_core.entity.MongodbConfig;
import com.xerecter.xrate.xrate_core.entity.MySQLConfig;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;


@Data
@Accessors(chain = true)
@Component
@ConfigurationProperties("com.xerecter.xrate")
public class XrateProperties {

    @NestedConfigurationProperty
    private XrateConfig config = new XrateConfig();

    @NestedConfigurationProperty
    private MongodbConfig mongodbConfig = new MongodbConfig();

    @NestedConfigurationProperty
    private MySQLConfig mySQLConfig = new MySQLConfig();

}
