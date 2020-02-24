package com.xerecter.xrate.xrate_core.factory;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.util.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;

/**
 * xrate配置文件工厂
 *
 * @author xdd
 */
public class XrateConfigFactory {

    /**
     * 获取xrate配置文件BeanDefinition
     *
     * @param config              最初配置
     * @param serverProperties    服务器配置
     * @param standardEnvironment 环境配置
     * @return 配置文件BeanDefinition
     */
    public static XrateConfig getXrateConfig(
            XrateConfig config,
            ServerProperties serverProperties,
            StandardEnvironment standardEnvironment
    ) {
        String serviceName = null;
        String serviceId = null;
        if (StringUtils.isBlank(config.getServiceName())) {
            serviceName = standardEnvironment.getProperty("spring.application.name");
            Assert.isTrue(StringUtils.isNotBlank(serviceName), "serviceName is empty");
        } else {
            serviceName = config.getServiceName();
        }
        if (StringUtils.isBlank(config.getServiceId())) {
            Assert.isTrue(serverProperties != null, "must set service id");
            int serverPort = serverProperties.getPort();
            String currIp = CommonUtil.getHostIp();
            serviceId = currIp + ":" + serverPort;
        } else {
            serviceId = config.getServiceId();
        }
        Assert.isTrue(config.getPersistenceConfig() != null, "persistence config is null");
        XrateConfig xrateConfig = new XrateConfig();
        xrateConfig.setSerializerWay(config.getSerializerWay());
        xrateConfig.setAsyncInvoke(config.getAsyncInvoke());
        xrateConfig.setPersistenceWay(config.getPersistenceWay());
        xrateConfig.setServiceName(serviceName);
        xrateConfig.setServiceId(serviceId);
        xrateConfig.setPersistenceConfig(config.getPersistenceConfig());
        xrateConfig.setRetryTimes(config.getRetryTimes());
        xrateConfig.setAsyncBufferSize(config.getAsyncBufferSize());
        xrateConfig.setInitCheckInterval(config.getInitCheckInterval());
        xrateConfig.setRetryInterval(config.getRetryInterval());
        xrateConfig.setRetryTimes(config.getRetryTimes());
        xrateConfig.setDebugMode(config.getDebugMode());
        return xrateConfig;
    }

}
