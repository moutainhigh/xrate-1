package io.github.xerecter.xrate.spring_boot_starter.config;

import io.github.xerecter.xrate.xrate_core.entity.MongodbConfig;
import io.github.xerecter.xrate.xrate_core.entity.MySQLConfig;
import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;


@Data
@Accessors(chain = true)
@Component
@ConfigurationProperties("io.github.xerecter.xrate")
public class XrateProperties {

    @NestedConfigurationProperty
    private XrateConfig config = new XrateConfig();

    @NestedConfigurationProperty
    private MongodbConfig mongodbConfig = new MongodbConfig();

    @NestedConfigurationProperty
    private MySQLConfig mySQLConfig = new MySQLConfig();

}
