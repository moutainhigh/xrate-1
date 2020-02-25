package io.github.xerecter.xrate.xrate_core.service.impl;

import io.github.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.dto.TransactionInfoDto;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import io.github.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import io.github.xerecter.xrate.xrate_core.service.ITransactionExecuterService;
import io.github.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import io.github.xerecter.xrate.xrate_core.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Data
@Slf4j
public class TransactionExecuterServiceImpl implements ITransactionExecuterService {

    @Autowired(required = false)
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired(required = false)
    @Qualifier("transactionInfoService")
    ITransactionInfoService transactionInfoService;

    @Autowired(required = false)
    @Qualifier("objectSerializerService")
    IObjectSerializerService objectSerializerService;

    public TransactionExecuterServiceImpl() {
    }

    @Override
    public Object executeNewTransaction(ProceedingJoinPoint point) {
        TransactionInfo transactionInfo = initTransaction(point);
        TransactionUtil.setCurrTransactionInfo(transactionInfo);
        TransactionUtil.setIsStartSide(CommonConstants.IS_START_SIDE);
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionDefinition.setName(CommonConstants.PRO_NAME_PREFIX + transactionInfo.getTransId());
        transactionDefinition.setTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
        transactionDefinition.setReadOnly(false);
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        Object result = null;
        try {
            result = point.proceed();
            dataSourceTransactionManager.commit(transactionStatus);
            if (!transactionInfoService.updateTransactionNeedSuccessAndStatus(transactionInfo.getTransId(), true, CommonConstants.TRANS_SUCCESS_STATUS)) {
                log.error("update success transaction error -> " + transactionInfo.getTransId());
            } else {
                TransactionUtil.printDebugInfo(() -> log.info("update success transaction success -> " + transactionInfo.getTransId()));
            }
            transactionInfo.setTransStatus(CommonConstants.TRANS_SUCCESS_STATUS);
            transactionInfo.setNeedSuccess(true);
            this.executeStartSideSuccessTransaction(transactionInfo);
            log.info("xrate transaction success -> " + transactionInfo.getTransId());
        } catch (Throwable throwable) {
            dataSourceTransactionManager.rollback(transactionStatus);
            if (!transactionInfoService.updateTransactionNeedCancelAndStatus(transactionInfo.getTransId(), true, CommonConstants.TRANS_CANCEL_STATUS)) {
                log.error("update cancel transaction error -> " + transactionInfo.getTransId());
            } else {
                TransactionUtil.printDebugInfo(() -> log.info("update cancel transaction success -> " + transactionInfo.getTransId()));
            }
            transactionInfo.setTransStatus(CommonConstants.TRANS_CANCEL_STATUS);
            transactionInfo.setNeedCancel(true);
            this.executeStartSideCancelTransaction(transactionInfo);
            log.info("xrate transaction cancel -> " + transactionInfo.getTransId());
            throw new IllegalArgumentException(throwable);
        } finally {
            TransactionUtil.removeAll();
        }
        return result;
    }

    @Override
    public Object executeStartedTransaction(ProceedingJoinPoint point) {
        XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
            TransactionInfo transactionInfo = transactionInfoService.getTransactionInfo(currTransactionInfo.getTransId(), xrateConfig.getServiceId());
            if (transactionInfo == null) {
                transactionInfo = this.initStartedTransaction(point);
                TransactionUtil.setCurrTransactionInfo(transactionInfo);
            }
            if (CommonConstants.TRANS_INIT_STATUS == transactionInfo.getTransStatus()) {
                DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
                transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                transactionDefinition.setName(CommonConstants.PRO_NAME_PREFIX + transactionInfo.getTransId());
                transactionDefinition.setTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
                transactionDefinition.setReadOnly(false);
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                Object result = null;
                try {
                    result = point.proceed();
                    byte[] resultBytes = objectSerializerService.serializerObject(result);
                    dataSourceTransactionManager.commit(transactionStatus);
                    if (!transactionInfoService.updateTransactionStatusAndResult(
                            transactionInfo.getTransId(),
                            CommonConstants.TRANS_SUCCESS_STATUS, resultBytes)) {
                        log.error("update trans status error tans_id is -> " + transactionInfo.getTransId());
                    } else {
                        String transId = transactionInfo.getTransId();
                        TransactionUtil.printDebugInfo(() -> log.info("update trans status success tans_id is -> " + transId));
                    }
                    transactionInfo.setTransStatus(CommonConstants.TRANS_SUCCESS_STATUS);
                    transactionInfo.setResult(resultBytes);
                } catch (Throwable throwable) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    transactionInfoService.updateTransactionStatus(transactionInfo.getTransId(), CommonConstants.TRANS_CANCEL_STATUS);
                    transactionInfo.setTransStatus(CommonConstants.TRANS_CANCEL_STATUS);
                    throw new IllegalArgumentException(throwable);
                } finally {
                    HttpServletResponse response = TransactionUtil.getCurrHttpServletResponse();
                    if (response != null) {
                        response.setHeader(CommonConstants.TRANS_POSITION_KEY + "_" + TransactionUtil.getCurrMbPosition(),
                                String.valueOf(TransactionUtil.getCurrMbPosition()));
                        TransactionUtil.printDebugInfo(() -> log.info("set response header success"));
                    }
                }
                return result;
            } else if (CommonConstants.TRANS_SUCCESS_STATUS == transactionInfo.getTransStatus()) {
                String transId = transactionInfo.getTransId();
                TransactionUtil.printDebugInfo(() -> log.info("transaction already execute direct return result -> " + transId));
                return objectSerializerService.deserializerObject(transactionInfo.getResult());
            } else {
                try {
                    return point.proceed();
                } catch (Throwable throwable) {
                    throw new IllegalArgumentException(throwable);
                }
            }
        } else if (CommonConstants.TRANS_CANCEL_STATUS == currTransactionInfo.getTransStatus()) {
            TransactionUtil.printDebugInfo(() -> log.info("started cancel -> " + currTransactionInfo.getTransId()));
            TransactionInfo transactionInfo = transactionInfoService.getTransactionInfo(currTransactionInfo.getTransId(), xrateConfig.getServiceId());
            if (transactionInfo != null) {
                this.executeStartedSideCancelTransaction(transactionInfo);
            } else {
                log.error("not get transaction -> " + currTransactionInfo.getTransId());
            }
            try {
                Class<?> beanClass = Class.forName(transactionInfo.getBeanClassName());
                Class<?>[] paramClasses = ReflectUtil.getClassesByNames(CommonUtil.getArrayByList(String.class, transactionInfo.getParamClassNames()));
                Method tryMethod = beanClass.getDeclaredMethod(transactionInfo.getTryName(), paramClasses);
                return ReflectUtil.getClassDefaultValue(tryMethod.getReturnType());
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            return null;
        } else if (CommonConstants.TRANS_SUCCESS_STATUS == currTransactionInfo.getTransStatus()) {
            TransactionUtil.printDebugInfo(() -> log.info("started success -> " + currTransactionInfo.getTransId()));
            TransactionInfo transactionInfo = transactionInfoService.getSimpleTransactionInfo(currTransactionInfo.getTransId(), xrateConfig.getServiceId());
            if (transactionInfo != null) {
                this.executeStartedSideSuccessTransaction(transactionInfo);
            } else {
                log.error("not get transaction -> " + currTransactionInfo.getTransId());
            }
            try {
                Class<?> beanClass = Class.forName(transactionInfo.getBeanClassName());
                Class<?>[] paramClasses = ReflectUtil.getClassesByNames(CommonUtil.getArrayByList(String.class, transactionInfo.getParamClassNames()));
                Method tryMethod = beanClass.getDeclaredMethod(transactionInfo.getTryName(), paramClasses);
                return ReflectUtil.getClassDefaultValue(tryMethod.getReturnType());
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            return null;
        }
        try {
            return point.proceed();
        } catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable);
        }
    }

    @Override
    public TransactionInfo initTransaction(ProceedingJoinPoint point) {
        Signature signature = point.getSignature();
        TransactionInfo transactionInfo = new TransactionInfo();
        String transId = SnowflakeKeyGenerator.getInstance().generateKey().longValue() + "-" + 0;
        transactionInfo.setTransId(transId);
        transactionInfo.setIsStart(true);
        initTransaction(point, signature, transactionInfo);
        return this.transactionInfoService.addTransactionInfo(transactionInfo);
    }

    @Override
    public TransactionInfo initStartedTransaction(ProceedingJoinPoint point) {
        Signature signature = point.getSignature();
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTransId(currTransactionInfo.getTransId());
        transactionInfo.setIsStart(false);
        initTransaction(point, signature, transactionInfo);
        return this.transactionInfoService.addTransactionInfo(transactionInfo);
    }

    @Override
    public void executeStartSideCancelTransaction(TransactionInfo transactionInfo) {
        XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
        setXrateConfigByTransactionInfo(xrateConfig, transactionInfo);
        TransactionUtil.setCurrXrateConfig(xrateConfig);
        if (transactionInfo.getNeedCancel()) {
            TransactionUtil.printDebugInfo(() -> log.info("async invoke -> " + xrateConfig.getAsyncInvoke()));
            TransactionUtil.printDebugInfo(() -> log.info("retry times -> " + xrateConfig.getRetryTimes()));
            TransactionUtil.printDebugInfo(() -> log.info("retry interval -> " + xrateConfig.getRetryInterval()));
            if (xrateConfig.getAsyncInvoke()) {
                TransactionUtil.publishAnTransactionProcessor(transactionInfo, this::startSideCancelTransaction);
            } else {
                TransactionInfoDto transactionInfoDto = new TransactionInfoDto();
                transactionInfoDto.setRetryTimes(0);
                transactionInfoDto.setExecutor(this::startSideCancelTransaction);
                transactionInfoDto.setTransactionInfo(transactionInfo);
                this.startSideCancelTransaction(transactionInfoDto);
            }
        } else {
            throw new IllegalArgumentException("transaction status error");
        }
    }

    @Override
    public void executeStartedSideCancelTransaction(TransactionInfo transactionInfo) {
        XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
        setXrateConfigByTransactionInfo(xrateConfig, transactionInfo);
        TransactionUtil.setCurrXrateConfig(xrateConfig);
        if (!this.transactionInfoService.updateTransactionNeedCancel(transactionInfo.getTransId(), true)) {
            log.error("update trans cancel error -> " + transactionInfo.getTransId());
        } else {
            transactionInfo.setNeedCancel(true);
            TransactionUtil.printDebugInfo(() -> log.info("update trans cancel success -> " + transactionInfo.getTransId()));
        }
        if (xrateConfig.getAsyncInvoke()) {
            TransactionUtil.publishAnTransactionProcessor(transactionInfo, this::startedSideCancelTransaction);
        } else {
            TransactionInfoDto transactionInfoDto = new TransactionInfoDto();
            transactionInfoDto.setExecutor(this::startedSideCancelTransaction);
            transactionInfoDto.setTransactionInfo(transactionInfo);
            transactionInfoDto.setRetryTimes(0);
            this.startedSideCancelTransaction(transactionInfoDto);
        }
    }

    @Override
    public void removeTransactionAndMembers(TransactionInfo transactionInfo) {
        this.transactionInfoService.removeTransactionInfo(transactionInfo.getTransId());
        this.transactionInfoService.removeTransactionMembers(transactionInfo.getTransId());
    }

    @Override
    public void executeStartSideSuccessTransaction(TransactionInfo transactionInfo) {
        XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
        setXrateConfigByTransactionInfo(xrateConfig, transactionInfo);
        TransactionUtil.setCurrXrateConfig(xrateConfig);
        TransactionUtil.printDebugInfo(() -> log.info("async invoke -> " + xrateConfig.getAsyncInvoke()));
        TransactionUtil.printDebugInfo(() -> log.info("retry times -> " + xrateConfig.getRetryTimes()));
        TransactionUtil.printDebugInfo(() -> log.info("retry interval -> " + xrateConfig.getRetryInterval()));
        if (transactionInfo.getNeedSuccess()) {
            if (xrateConfig.getAsyncInvoke()) {
                TransactionUtil.publishAnTransactionProcessor(transactionInfo, this::startSideSuccessTransaction);
            } else {
                TransactionInfoDto transactionInfoDto = new TransactionInfoDto();
                transactionInfoDto.setRetryTimes(0);
                transactionInfoDto.setExecutor(this::startSideSuccessTransaction);
                transactionInfoDto.setTransactionInfo(transactionInfo);
                this.startSideSuccessTransaction(transactionInfoDto);
            }
        } else {
            throw new IllegalArgumentException("transaction status error");
        }
    }

    @Override
    public void executeStartedSideSuccessTransaction(TransactionInfo transactionInfo) {
        XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
        setXrateConfigByTransactionInfo(xrateConfig, transactionInfo);
        TransactionUtil.setCurrXrateConfig(xrateConfig);
        TransactionUtil.printDebugInfo(() -> log.info("async invoke -> " + xrateConfig.getAsyncInvoke()));
        TransactionUtil.printDebugInfo(() -> log.info("retry times -> " + xrateConfig.getRetryTimes()));
        TransactionUtil.printDebugInfo(() -> log.info("retry interval -> " + xrateConfig.getRetryInterval()));
        if (!this.transactionInfoService.updateTransactionNeedSuccess(transactionInfo.getTransId(), true)) {
            log.error("update transaction error -> " + transactionInfo.getTransId());
        } else {
            transactionInfo.setNeedSuccess(true);
        }
        if (xrateConfig.getAsyncInvoke()) {
            TransactionUtil.publishAnTransactionProcessor(transactionInfo, this::startedSideSuccessTransaction);
        } else {
            TransactionInfoDto transactionInfoDto = new TransactionInfoDto();
            transactionInfoDto.setTransactionInfo(transactionInfo);
            transactionInfoDto.setExecutor(this::startedSideSuccessTransaction);
            transactionInfoDto.setRetryTimes(0);
            this.startedSideSuccessTransaction(transactionInfoDto);
        }
        TransactionUtil.printDebugInfo(() -> log.info("update transaction success -> " + transactionInfo.getTransId()));
    }

    private void startSideSuccessTransaction(TransactionInfoDto transactionInfoDto) {
        try {
            XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
            TransactionUtil.printDebugInfo(() -> log.info("execute start success retries -> " + transactionInfoDto.getRetryTimes() + " trans id ->" + transactionInfoDto.getTransactionInfo().getTransId()));
            if (transactionInfoDto.getRetryTimes() <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                TransactionInfo transactionInfo = transactionInfoDto.getTransactionInfo();
                TransactionUtil.setIsStartSide(CommonConstants.IS_START_SIDE);
                try {
                    this.successTransaction(transactionInfo);
                } catch (Exception e) {
                    int retryTimes = transactionInfoDto.getRetryTimes() + 1;
                    log.error("execute start success error -> " + transactionInfoDto.getTransactionInfo().getTransId());
                    if (retryTimes <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                        if (xrateConfig.getAsyncInvoke()) {
                            ScheduledExecutorService transactionScheduled = TransactionUtil.getTransactionScheduled();
                            transactionScheduled.schedule(() -> {
                                TransactionUtil.publishAnTransactionProcessor(transactionInfo, retryTimes, false, false, this::startSideSuccessTransaction);
                            }, xrateConfig.getRetryInterval() * retryTimes, TimeUnit.SECONDS);
                        } else {
                            transactionInfoDto.setRetryTimes(retryTimes);
                            Thread.sleep(1000 * xrateConfig.getRetryInterval() * retryTimes);
                            this.startSideSuccessTransaction(transactionInfoDto);
                        }
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            TransactionUtil.removeAll();
        }
    }

    private void startedSideSuccessTransaction(TransactionInfoDto transactionInfoDto) {
        try {
            XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
            TransactionUtil.printDebugInfo(() -> log.info("execute started success retries -> " + transactionInfoDto.getRetryTimes() + " trans id -> " + transactionInfoDto.getTransactionInfo().getTransId()));
            if (transactionInfoDto.getRetryTimes() <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                TransactionInfo transactionInfo = transactionInfoDto.getTransactionInfo();
                TransactionUtil.setIsStartSide(CommonConstants.NOT_START_SIDE);
                try {
                    this.successTransaction(transactionInfo);
                } catch (Exception e) {
                    log.error("execute started success error -> " + transactionInfoDto.getTransactionInfo().getTransId());
                    int retryTimes = transactionInfoDto.getRetryTimes() + 1;
                    if (retryTimes <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                        if (xrateConfig.getAsyncInvoke()) {
                            ScheduledExecutorService transactionScheduled = TransactionUtil.getTransactionScheduled();
                            transactionScheduled.schedule(() -> {
                                TransactionUtil.publishAnTransactionProcessor(transactionInfo, retryTimes, false, false, this::startedSideSuccessTransaction);
                            }, xrateConfig.getRetryInterval() * retryTimes, TimeUnit.SECONDS);
                        } else {
                            transactionInfoDto.setRetryTimes(retryTimes);
                            Thread.sleep(1000 * xrateConfig.getRetryInterval() * retryTimes);
                            this.startedSideSuccessTransaction(transactionInfoDto);
                        }
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            TransactionUtil.removeAll();
        }
    }

    private void successTransaction(TransactionInfo transactionInfo) throws Exception {
        TransactionUtil.setCurrTransactionInfo(transactionInfo);
        Iterator<TransactionMember> iterator = transactionInfo.getTransactionMembers().iterator();
        Exception e = null;
        while (iterator.hasNext()) {
            try {
                if (e == null) {
                    TransactionMember transactionMember = iterator.next();
                    TransactionUtil.setCurrTransMb(transactionMember);
                    Class<?> memberClass = Class.forName(transactionMember.getMemberClassName());
                    Class<?>[] memberParamClasses = ReflectUtil.getClassesByNames(CommonUtil.getArrayByList(String.class, transactionMember.getParamClassNames()));
                    Object[] params = new Object[memberParamClasses.length];
                    for (int i = 0; i < params.length; i++) {
                        params[i] = ReflectUtil.getClassDefaultValue(memberParamClasses[i]);
                    }
                    Object bean = BeanUtil.getSpringCtx().getBean(memberClass);
                    Method tryMethod = memberClass.getDeclaredMethod(transactionMember.getTryName(), memberParamClasses);
                    tryMethod.setAccessible(true);
                    tryMethod.invoke(bean, params);
                } else {
                    iterator.next();
                }
            } catch (Exception ex) {
                e = ex;
            }
        }
        if (e == null) {
            TransactionUtil.printDebugInfo(() -> log.info("transaction success -> " + transactionInfo.getTransId()));
            this.removeTransactionAndMembers(transactionInfo);
            TransactionUtil.printDebugInfo(() -> log.info("success db main remove success -> " + transactionInfo.getTransId()));
        } else {
            throw e;
        }
    }

    private void startedSideCancelTransaction(TransactionInfoDto transactionInfoDto) {
        try {
            XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
            TransactionUtil.printDebugInfo(() -> log.info("execute started cancel retries -> " + transactionInfoDto.getRetryTimes() + " trans id -> " + transactionInfoDto.getTransactionInfo().getTransId()));
            if (transactionInfoDto.getRetryTimes() <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                TransactionInfo transactionInfo = transactionInfoDto.getTransactionInfo();
                TransactionUtil.setCurrTransactionInfo(transactionInfo);
                TransactionUtil.setIsStartSide(CommonConstants.NOT_START_SIDE);
                TransactionUtil.printDebugInfo(() -> log.info(" transaction need cancel -> " + transactionInfo.getNeedCancel()));
                try {
                    if (CommonConstants.TRANS_CANCEL_STATUS == transactionInfo.getTransStatus() ||
                            CommonConstants.TRANS_INIT_STATUS == transactionInfo.getTransStatus()) {
                        transactionMembersCancel(transactionInfo.getTransactionMembers());
                        TransactionUtil.printDebugInfo(() -> log.info("cancel transaction -> " + transactionInfo.getTransId()));
                        if (this.transactionInfoService.updateTransactionStatus(transactionInfo.getTransId(), CommonConstants.TRANS_FAIL_STATUS)) {
                            this.removeTransactionAndMembers(transactionInfo);
                            TransactionUtil.printDebugInfo(() -> log.info("cancel db main and mb remove success -> " + transactionInfo.getTransId()));
                        }
                    } else if (CommonConstants.TRANS_SUCCESS_STATUS == transactionInfo.getTransStatus()) {
                        Exception e1 = null;
                        Exception e2 = null;
                        if (!transactionInfoDto.isMainCancel()) {
                            try {
                                Class<?> beanClass = Class.forName(transactionInfo.getBeanClassName());
                                Class<?>[] paramClasses = new Class[transactionInfo.getParamClassNames().size()];
                                for (int j = 0; j < transactionInfo.getParamClassNames().size(); j++) {
                                    paramClasses[j] = Class.forName(transactionInfo.getParamClassNames().get(j));
                                }
                                Object[] params = (Object[]) objectSerializerService.deserializerObject(transactionInfo.getParams());
                                String cancelName = transactionInfo.getCancelName();
                                Method cancelMethod = beanClass.getDeclaredMethod(cancelName, paramClasses);
                                cancelMethod.setAccessible(true);
                                Object bean = BeanUtil.getSpringCtx().getBean(beanClass);
                                DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
                                transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                                transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                                transactionDefinition.setName(CommonConstants.PRO_NAME_PREFIX + transactionInfo.getTransId());
                                transactionDefinition.setTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
                                transactionDefinition.setReadOnly(false);
                                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                                try {
                                    cancelMethod.invoke(bean, params);
                                    dataSourceTransactionManager.commit(transactionStatus);
                                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                    dataSourceTransactionManager.rollback(transactionStatus);
                                    throw e;
                                }
                                this.transactionInfoService.updateTransactionStatus(transactionInfo.getTransId(), CommonConstants.TRANS_CANCEL_STATUS);
                                transactionInfo.setTransStatus(CommonConstants.TRANS_CANCEL_STATUS);
                                TransactionUtil.printDebugInfo(() -> log.info("cancel transaction main -> " + transactionInfo.getTransId()));
                            } catch (Exception e) {
                                e1 = e;
                            }
                        }
                        if (!transactionInfoDto.isMemberCancel()) {
                            try {
                                transactionMembersCancel(transactionInfo.getTransactionMembers());
                                TransactionUtil.printDebugInfo(() -> log.info("cancel transaction sub -> " + transactionInfo.getTransId()));
                            } catch (Exception e) {
                                e2 = e;
                            }
                        }
                        if (e1 != null && e2 != null) {
                            Exception exception = new Exception();
                            exception.addSuppressed(e1);
                            exception.addSuppressed(e2);
                            throw exception;
                        }
                        if (e1 == null) {
                            transactionInfoDto.setMainCancel(true);
                            this.transactionInfoService.removeTransactionInfo(transactionInfo.getTransId());
                            TransactionUtil.printDebugInfo(() -> log.info("cancel db main remove success -> " + transactionInfo.getTransId()));
                        }
                        if (e2 == null) {
                            transactionInfoDto.setMemberCancel(true);
                            this.transactionInfoService.removeTransactionMembers(transactionInfo.getTransId());
                            TransactionUtil.printDebugInfo(() -> log.info("cancel db mb remove success -> " + transactionInfo.getTransId()));
                        }
                        if (e1 != null) {
                            throw e1;
                        } else if (e2 != null) {
                            throw e2;
                        }
                    }
                } catch (Exception e) {
                    int retryTimes = transactionInfoDto.getRetryTimes() + 1;
                    log.error("execute started cancel error retrying -> " + transactionInfo.getTransId());
                    if (retryTimes <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                        if (xrateConfig.getAsyncInvoke()) {
                            TransactionUtil.getTransactionScheduled().schedule(() -> {
                                TransactionUtil.publishAnTransactionProcessor(transactionInfo, retryTimes, false, false, this::startedSideCancelTransaction);
                            }, retryTimes * xrateConfig.getRetryInterval(), TimeUnit.SECONDS);
                        } else {
                            transactionInfoDto.setRetryTimes(retryTimes);
                            Thread.sleep(1000 * retryTimes * xrateConfig.getRetryInterval());
                            this.startedSideCancelTransaction(transactionInfoDto);
                        }
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            TransactionUtil.removeAll();
        }
    }

    private void startSideCancelTransaction(TransactionInfoDto transactionInfoDto) {
        try {
            XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
            TransactionUtil.printDebugInfo(() -> log.info("execute start cancel retries -> " + transactionInfoDto.getRetryTimes() + " trans is -> " + transactionInfoDto.getTransactionInfo().getTransId()));
            if (transactionInfoDto.getRetryTimes() <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                TransactionInfo transactionInfo = transactionInfoDto.getTransactionInfo();
                TransactionUtil.setCurrTransactionInfo(transactionInfo);
                TransactionUtil.setIsStartSide(CommonConstants.IS_START_SIDE);
                try {
                    transactionMembersCancel(transactionInfo.getTransactionMembers());
                    TransactionUtil.printDebugInfo(() -> log.info("cancel transaction -> " + transactionInfo.getTransId()));
                    if (this.transactionInfoService.updateTransactionStatus(transactionInfo.getTransId(), CommonConstants.TRANS_FAIL_STATUS)) {
                        this.removeTransactionAndMembers(transactionInfo);
                    }
                } catch (Exception e) {
                    int retryTimes = transactionInfoDto.getRetryTimes() + 1;
                    log.error("execute start cancel error retrying -> " + transactionInfo.getTransId());
                    if (retryTimes <= xrateConfig.getRetryTimes() || xrateConfig.getRetryTimes() == -1) {
                        if (xrateConfig.getAsyncInvoke()) {
                            ScheduledExecutorService transactionScheduled = TransactionUtil.getTransactionScheduled();
                            transactionScheduled.schedule(() -> {
                                TransactionUtil.publishAnTransactionProcessor(transactionInfo, retryTimes, false, false, this::startSideCancelTransaction);
                            }, xrateConfig.getRetryInterval() * retryTimes, TimeUnit.SECONDS);
                        } else {
                            transactionInfoDto.setRetryTimes(retryTimes);
                            Thread.sleep(1000 * xrateConfig.getRetryInterval() * retryTimes);
                            this.startSideCancelTransaction(transactionInfoDto);
                        }
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            TransactionUtil.removeAll();
        }
    }

    private void transactionMembersCancel(List<TransactionMember> transactionMembers) throws Exception {
        Iterator<TransactionMember> iterator = transactionMembers.iterator();
        Exception e = null;
        while (iterator.hasNext()) {
            try {
                if (e == null) {
                    TransactionMember transactionMember = iterator.next();
                    TransactionUtil.setCurrTransMb(transactionMember);
                    TransactionUtil.printDebugInfo(() -> log.info("cancel transaction execute before -> " + transactionMember.getTransId()));
                    Class<?> memberClass = Class.forName(transactionMember.getMemberClassName());
                    Class<?>[] memberParamClasses = ReflectUtil.getClassesByNames(CommonUtil.getArrayByList(String.class, transactionMember.getParamClassNames()));
                    Object bean = BeanUtil.getSpringCtx().getBean(memberClass);
                    Method tryMethod = memberClass.getDeclaredMethod(transactionMember.getTryName(), memberParamClasses);
                    tryMethod.setAccessible(true);
                    tryMethod.invoke(bean, (Object[]) objectSerializerService.deserializerObject(transactionMember.getParams()));
                    TransactionUtil.printDebugInfo(() -> log.info("cancel transaction member -> " + transactionMember.getTransId()));
                }
            } catch (Exception ex) {
                e = ex;
            }
        }
        if (e != null) {
            throw e;
        }
    }

    /**
     * 通过事务信息来设置配置
     *
     * @param xrateConfig     配置内容
     * @param transactionInfo 事务信息
     */
    private void setXrateConfigByTransactionInfo(XrateConfig xrateConfig, TransactionInfo transactionInfo) {
        try {
            Class<?>[] paramsClasses = new Class<?>[transactionInfo.getParamClassNames().size()];
            for (int i = 0; i < transactionInfo.getParamClassNames().size(); i++) {
                paramsClasses[i] = Class.forName(transactionInfo.getParamClassNames().get(i));
            }
            Method method = Class.forName(transactionInfo.getBeanClassName()).getDeclaredMethod(transactionInfo.getTryName(), paramsClasses);
            setXrateConfigByAnnotation(method.getAnnotation(XrateTransaction.class), xrateConfig);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据注解来设置配置
     *
     * @param xrateTransaction 注解内容
     * @param xrateConfig      要设置的配置
     */
    private void setXrateConfigByAnnotation(
            XrateTransaction xrateTransaction,
            XrateConfig xrateConfig
    ) {
        if (xrateTransaction.asyncInvoke() != XrateTransaction.InvokeEnum.NONE) {
            if (xrateTransaction.asyncInvoke() == XrateTransaction.InvokeEnum.ASYNC) {
                xrateConfig.setAsyncInvoke(true);
            } else if (xrateTransaction.asyncInvoke() == XrateTransaction.InvokeEnum.SYNC) {
                xrateConfig.setAsyncInvoke(false);
            }
        }
        if (xrateTransaction.retryTimes() != -1) {
            xrateConfig.setRetryTimes(xrateTransaction.retryTimes());
        }
        if (xrateTransaction.retryInterval() != -1) {
            xrateConfig.setRetryInterval(xrateTransaction.retryInterval());
        }
    }

    private void initTransaction(ProceedingJoinPoint point, Signature signature, TransactionInfo transactionInfo) {
        XrateConfig xrateConfig = TransactionUtil.getCurrXrateConfig();
        transactionInfo.setNeedCancel(false);
        transactionInfo.setHoldServiceId(xrateConfig.getServiceId());
        transactionInfo.setTransStatus(CommonConstants.TRANS_INIT_STATUS);
        transactionInfo.setTryName(signature.getName());
        Class[] clazzs = new Class[point.getArgs().length];
        for (int i = 0; i < point.getArgs().length; i++) {
            clazzs[i] = point.getArgs()[i].getClass();
        }
        Method pointMethod = null;
        try {
            pointMethod = point.getTarget().getClass().getMethod(signature.getName(), clazzs);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        XrateTransaction xrateTransaction = pointMethod.getAnnotation(XrateTransaction.class);
        String cancelMethodName;
        if (StringUtils.isNotBlank(xrateTransaction.value())) {
            cancelMethodName = xrateTransaction.value();
        } else {
            cancelMethodName = xrateTransaction.cancelMethod();
        }
        setXrateConfigByAnnotation(xrateTransaction, xrateConfig);
        Assert.isTrue(StringUtils.isNoneBlank(cancelMethodName), "cancel method is null");
        transactionInfo.setCancelName(cancelMethodName);
        transactionInfo.setBeanClassName(signature.getDeclaringTypeName());
        List<String> paramClassNames = new ArrayList<>(point.getArgs().length);
        for (int i = 0; i < point.getArgs().length; i++) {
            paramClassNames.add(i, point.getArgs()[i].getClass().getName());
        }
        transactionInfo.setParamClassNames(paramClassNames);
        transactionInfo.setParams(objectSerializerService.serializerObject(point.getArgs()));
        transactionInfo.setTransactionMembers(new ArrayList<>());
        TransactionUtil.setCurrXrateConfig(xrateConfig);
    }
}
