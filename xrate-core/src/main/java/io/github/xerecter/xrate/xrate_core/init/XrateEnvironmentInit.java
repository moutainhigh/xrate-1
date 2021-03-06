package io.github.xerecter.xrate.xrate_core.init;

import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import io.github.xerecter.xrate.xrate_core.factory.TransactionInfoServiceFactory;
import io.github.xerecter.xrate.xrate_core.factory.ObjectSerializerFactory;
import io.github.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import io.github.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import io.github.xerecter.xrate.xrate_core.service.impl.TransactionExecuterServiceImpl;
import io.github.xerecter.xrate.xrate_core.util.BeanUtil;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
                    Objects.requireNonNull(TransactionInfoServiceFactory.getITransactionInfoServiceDefinition(config, config.getPersistenceConfig())));
        }
        TransactionExecuterServiceImpl transactionExecuterService = BeanUtil.getSpringCtx().getBean(TransactionExecuterServiceImpl.class);
        transactionExecuterService.setObjectSerializerService(BeanUtil.getSpringCtx().getBean("objectSerializerService", IObjectSerializerService.class));
        transactionExecuterService.setDataSourceTransactionManager(BeanUtil.getSpringCtx().getBean(DataSourceTransactionManager.class));
        transactionExecuterService.setTransactionInfoService(BeanUtil.getSpringCtx().getBean("transactionInfoService", ITransactionInfoService.class));
        log.info("xrate start success");
    }

}
