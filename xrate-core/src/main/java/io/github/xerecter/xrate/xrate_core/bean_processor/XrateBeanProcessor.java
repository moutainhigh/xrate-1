package io.github.xerecter.xrate.xrate_core.bean_processor;

import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import io.github.xerecter.xrate.xrate_core.factory.XrateConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XrateBeanProcessor implements BeanPostProcessor {

    @Autowired
    ServerProperties serverProperties;

    @Autowired
    StandardEnvironment standardEnvironment;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof XrateConfig) {
            XrateConfig xrateConfig = (XrateConfig) bean;
            return XrateConfigFactory.getXrateConfig(xrateConfig, serverProperties, standardEnvironment);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
