package com.xerecter.xrate.xrate_core.init;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.factory.TransactionInfoServiceFactory;
import com.xerecter.xrate.xrate_core.factory.ObjectSerializerFactory;
import com.xerecter.xrate.xrate_core.factory.XrateConfigFactory;
import com.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import com.xerecter.xrate.xrate_core.service.ITransactionExecuterService;
import com.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import com.xerecter.xrate.xrate_core.service.impl.TransactionExecuteServiceImpl;
import com.xerecter.xrate.xrate_core.util.BeanUtil;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XrateEnvironmentInit implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XrateConfig config = applicationContext.getBean(XrateConfig.class);
        BeanUtil.setSpringCtx(applicationContext);
        TransactionUtil.initTransactionDisruptor(config.getAsyncBufferSize());
        TransactionUtil.initTransactionScheduled(config.getAsyncBufferSize());
        TransactionUtil.setDebugMode(config.getDebugMode());
        if (config.getDebugMode()) {
            log.info("start debug mode");
        }
        if (applicationContext.getAutowireCapableBeanFactory() instanceof DefaultListableBeanFactory) {
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
            beanFactory.setAllowBeanDefinitionOverriding(true);

            beanFactory.registerBeanDefinition("objectSerializerService",
                    ObjectSerializerFactory.getObjectSerializerServiceDefinition(config.getSerializerWay()));

            beanFactory.registerBeanDefinition("transactionInfoService",
                    TransactionInfoServiceFactory.getITransactionInfoServiceDefinition(config, config.getPersistenceConfig()));
        }
        TransactionExecuteServiceImpl transactionExecuterService = BeanUtil.getSpringCtx().getBean(TransactionExecuteServiceImpl.class);
        transactionExecuterService.setObjectSerializerService(BeanUtil.getSpringCtx().getBean("objectSerializerService", IObjectSerializerService.class));
        transactionExecuterService.setDataSourceTransactionManager(BeanUtil.getSpringCtx().getBean(DataSourceTransactionManager.class));
        transactionExecuterService.setTransactionInfoService(BeanUtil.getSpringCtx().getBean("transactionInfoService", ITransactionInfoService.class));
        log.info("xrate start success");
    }

}
