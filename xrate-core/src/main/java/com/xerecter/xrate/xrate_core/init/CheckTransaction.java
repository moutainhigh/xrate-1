package com.xerecter.xrate.xrate_core.init;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.dto.TransactionInfoDto;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import com.xerecter.xrate.xrate_core.service.ITransactionExecuterService;
import com.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import com.xerecter.xrate.xrate_core.util.BeanUtil;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 检查事务，比如有那些事务未完成需要继续执行
 *
 * @author xdd
 */
@Slf4j
@Component
public class CheckTransaction implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(() -> {
            XrateConfig xrateConfig = BeanUtil.getSpringCtx().getBean(XrateConfig.class);
            if (xrateConfig.getNeedInitCheck()) {
                if (xrateConfig.getInitCheckInterval() > 0) {
                    try {
                        //log.info("check sleep -> " + xrateConfig.getInitCheckInterval());
                        Thread.sleep(1000 * xrateConfig.getInitCheckInterval());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ITransactionExecuterService transactionExecuterService = BeanUtil.getSpringCtx().getBean(ITransactionExecuterService.class);
                ITransactionInfoService transactionInfoService = BeanUtil.getSpringCtx().getBean(ITransactionInfoService.class);
                List<TransactionInfo> transactionInfos = transactionInfoService.getTransactionInfos(xrateConfig.getServiceId());
                transactionInfos.forEach((transactionInfo) -> {
                    if (transactionInfo.getIsStart()) {
                        if (CommonConstants.TRANS_INIT_STATUS == transactionInfo.getTransStatus()) {
                            //log.info("start need init -> " + transactionInfo.getTransId());
                            TransactionUtil.publishAnTransactionProcessor(transactionInfo, this::processUnSuccessInitTransaction);
                        } else if (CommonConstants.TRANS_SUCCESS_STATUS == transactionInfo.getTransStatus()) {
                            //log.info("start need success -> " + transactionInfo.getTransId());
                            transactionExecuterService.executeStartSideSuccessTransaction(transactionInfo);
                        } else if (CommonConstants.TRANS_CANCEL_STATUS == transactionInfo.getTransStatus()) {
                            //log.info("start need cancel -> " + transactionInfo.getTransId());
                            transactionExecuterService.executeStartSideCancelTransaction(transactionInfo);
                        }
                    } else {
                        if (transactionInfo.getNeedCancel()) {
                            //log.info("started need cancel -> " + transactionInfo.getTransId());
                            transactionExecuterService.executeStartedSideCancelTransaction(transactionInfo);
                        } else if (transactionInfo.getNeedSuccess()) {
                            //log.info("started need success -> " + transactionInfo.getTransId());
                            transactionExecuterService.executeStartedSideSuccessTransaction(transactionInfo);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 执行未成功执行的初始化事务操作
     *
     * @param transactionInfoDto 事务信息
     */
    private void processUnSuccessInitTransaction(TransactionInfoDto transactionInfoDto) {
        TransactionInfo transactionInfo = transactionInfoDto.getTransactionInfo();
        IObjectSerializerService objectSerializerService = BeanUtil.getSpringCtx().getBean(IObjectSerializerService.class);
        ITransactionInfoService transactionInfoService = BeanUtil.getSpringCtx().getBean(ITransactionInfoService.class);
        DataSourceTransactionManager dataSourceTransactionManager = BeanUtil.getSpringCtx().getBean(DataSourceTransactionManager.class);
        ITransactionExecuterService transactionExecuterService = BeanUtil.getSpringCtx().getBean(ITransactionExecuterService.class);
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionDefinition.setName(CommonConstants.PRO_NAME_PREFIX + transactionInfo.getTransId());
        transactionDefinition.setTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
        transactionDefinition.setReadOnly(false);
        try {
            Class<?> beanClass = Class.forName(transactionInfo.getBeanClassName());
            Object bean = BeanUtil.getSpringCtx().getBean(beanClass);
            Class<?>[] beanParamClasses = new Class[transactionInfo.getParamClassNames().size()];
            for (int i = 0; i < transactionInfo.getParamClassNames().size(); i++) {
                beanParamClasses[i] = Class.forName(transactionInfo.getParamClassNames().get(i));
            }
            Object[] params = (Object[]) objectSerializerService.deserializerObject(transactionInfo.getParams());
            Method tryMethod = beanClass.getDeclaredMethod(transactionInfo.getTryName(), beanParamClasses);
            TransactionUtil.setIsStartSide(CommonConstants.IS_START_SIDE);
            TransactionUtil.setCurrTransactionInfo(transactionInfo);
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            try {
                tryMethod.invoke(bean, params);
                dataSourceTransactionManager.commit(transactionStatus);
                if (!transactionInfoService.updateTransactionNeedSuccessAndStatus(transactionInfo.getTransId(), true, CommonConstants.TRANS_SUCCESS_STATUS)) {
                    //log.error("update success transaction error -> " + transactionInfo.getTransId());
                } else {
                    //log.info("update success transaction success -> " + transactionInfo.getTransId());
                }
                transactionInfo.setTransStatus(CommonConstants.TRANS_SUCCESS_STATUS);
                transactionInfo.setNeedSuccess(true);
                transactionExecuterService.executeStartSideSuccessTransaction(transactionInfo);
                //log.info("check trans init action success -> " + transactionInfo.getTransId());
            } catch (Exception e) {
                dataSourceTransactionManager.rollback(transactionStatus);
                if (!transactionInfoService.updateTransactionNeedCancelAndStatus(transactionInfo.getTransId(), true, CommonConstants.TRANS_CANCEL_STATUS)) {
                    //log.error("update cancel transaction error -> " + transactionInfo.getTransId());
                } else {
                    //log.info("update cancel transaction success -> " + transactionInfo.getTransId());
                }
                transactionInfo.setTransStatus(CommonConstants.TRANS_CANCEL_STATUS);
                transactionInfo.setNeedCancel(true);
                transactionExecuterService.executeStartSideCancelTransaction(transactionInfo);
                //log.info("check trans init action cancel -> " + transactionInfo.getTransId());
                throw new IllegalArgumentException(e);
            } finally {
                TransactionUtil.removeAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
