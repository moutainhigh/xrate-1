package com.xerecter.xrate.xrate_dubbo.filter;

import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.entity.TransactionMember;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import com.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import com.xerecter.xrate.xrate_core.util.BeanUtil;
import com.xerecter.xrate.xrate_core.util.CommonUtil;
import com.xerecter.xrate.xrate_core.util.ReflectUtil;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Activate(group = {org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE}, order = 0)
public class XrateDubboComsumerFilter implements Filter {

    private IObjectSerializerService objectSerializerService = null;

    private ITransactionInfoService transactionInfoService = null;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (CommonConstants.INIT_START_SIDE != TransactionUtil.getIsStartSide()) {
            Method targetMethod = null;
            try {
                targetMethod = invoker.getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            XrateTransaction xrateTransaction = null;
            if (targetMethod != null) {
                xrateTransaction = targetMethod.getAnnotation(XrateTransaction.class);
            }
            if (xrateTransaction != null) {
                TransactionUtil.printDebugInfo(() -> log.info("method is marked XrateTransaction"));
                IObjectSerializerService objectSerializerService = getObjectSerializerService();
                ITransactionInfoService transactionInfoService = getTransactionInfoService();
                TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
                XrateConfig xrateConfig;
                if (CommonConstants.IS_START_SIDE == TransactionUtil.getIsStartSide()) {
                    xrateConfig = TransactionUtil.getCurrXrateConfig();
                } else if (CommonConstants.NOT_START_SIDE == TransactionUtil.getIsStartSide()) {
                    xrateConfig = TransactionUtil.getCurrConnXrateConfig();
                } else {
                    xrateConfig = null;
                }
                invocation.setAttachment(CommonConstants.ASYNC_INVOKE_KEY, String.valueOf(xrateConfig.getAsyncInvoke()));
                TransactionUtil.printDebugInfo(() -> log.info("consumer curr async -> " + xrateConfig.getAsyncInvoke()));
                invocation.setAttachment(CommonConstants.RETRY_TIMES_KEY, String.valueOf(xrateConfig.getRetryTimes()));
                TransactionUtil.printDebugInfo(() -> log.info("consumer curr retry times -> " + xrateConfig.getRetryTimes()));
                invocation.setAttachment(CommonConstants.RETRY_INTERVAL_KEY, String.valueOf(xrateConfig.getRetryInterval()));
                TransactionUtil.printDebugInfo(() -> log.info("consumer curr retry interval -> " + xrateConfig.getRetryInterval()));
                if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
                    TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
                    String parentTransId = currTransactionInfo.getTransId();
                    int currMbPosition = TransactionUtil.incrCurrMbPosition();
                    String subTransId = parentTransId.split("-")[0] + "-" + currMbPosition;
                    Class<?> invokerInterface = invoker.getInterface();
                    currTransMb.setMemberClassName(invokerInterface.getName());
                    currTransMb.setParentTransId(parentTransId);
                    currTransMb.setTryName(invocation.getMethodName());
                    List<String> paramClassNames = CommonUtil.<String>getListByArray(ReflectUtil.getClassNamesByClasses(invocation.getParameterTypes()));
                    currTransMb.setParamClassNames(paramClassNames);
                    currTransMb.setParams(objectSerializerService.serializerObject(invocation.getArguments()));
                    if (currTransMb.getTransId() == null || (!subTransId.equals(currTransMb.getTransId()))) {
                        currTransMb.setTransId(subTransId);
                        currTransMb = transactionInfoService.addTransactionMember(currTransMb);
                        currTransactionInfo.getTransactionMembers().add(currTransMb);
                    }
                    String transId = currTransMb.getTransId();
                    TransactionUtil.printDebugInfo(() -> log.info("consumer provide trans id -> " + transId));
                    TransactionUtil.printDebugInfo(() -> log.info("consumer provide position id -> " + currMbPosition));
                    // 这里解释一下为什么这样写入 不知道为什么dubbo在写入同一key参数的时候不可以覆盖掉原来的参数
                    // 在传给下一个提供者的时候值仍然不变 这里这样写是为了让key唯一
                    invocation.setAttachment(CommonConstants.AWAIT_EXECUTE_METHOD_KEY,
                            CommonConstants.AWAIT_EXECUTE_TRY_METHOD);
                    invocation.setAttachment(CommonConstants.SUB_TRANS_ID_KEY + "_" + currMbPosition, String.valueOf(currTransMb.getTransId()));
                    invocation.setAttachment(CommonConstants.TRANS_POSITION_KEY + "_" + currMbPosition, String.valueOf(currMbPosition));
                    Result result = invoker.invoke(invocation);
                    List<String> keys = result.getAttachments().keySet().stream().filter(key ->
                            key.toLowerCase().startsWith(CommonConstants.TRANS_POSITION_KEY))
                            .sorted()
                            .collect(Collectors.toList());
                    int position = Integer.parseInt(result.getAttachments().get(keys.get(keys.size() - 1)));
                    TransactionUtil.printDebugInfo(() -> log.info("result position -> " + position));
                    TransactionUtil.setCurrMbPosition(position);
                    return result;
                } else if (CommonConstants.IS_START_SIDE == TransactionUtil.getIsStartSide() && CommonConstants.TRANS_CANCEL_STATUS == currTransactionInfo.getTransStatus()) {
                    onlySetSubTransId(invocation);
                    invocation.setAttachment(CommonConstants.AWAIT_EXECUTE_METHOD_KEY,
                            CommonConstants.AWAIT_EXECUTE_CANCEL_METHOD);
                    return invoker.invoke(invocation);
                } else if ((CommonConstants.NOT_START_SIDE == TransactionUtil.getIsStartSide() && currTransactionInfo.isNeedCancel())) {
                    onlySetSubTransId(invocation);
                    invocation.setAttachment(CommonConstants.AWAIT_EXECUTE_METHOD_KEY,
                            CommonConstants.AWAIT_EXECUTE_CANCEL_METHOD);
                    return invoker.invoke(invocation);
                } else if (CommonConstants.TRANS_SUCCESS_STATUS == currTransactionInfo.getTransStatus()) {
                    onlySetSubTransId(invocation);
                    invocation.setAttachment(CommonConstants.AWAIT_EXECUTE_METHOD_KEY,
                            CommonConstants.AWAIT_EXECUTE_SUCCESS_METHOD);
                    return invoker.invoke(invocation);
                }
            } else {
                TransactionUtil.printDebugInfo(() -> log.info("method is not mark XrateTransaction"));
            }
        }
        return invoker.invoke(invocation);
    }

    private void onlySetSubTransId(Invocation invocation) {
        TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
        String transId = currTransMb.getTransId();
        TransactionUtil.printDebugInfo(() -> log.info("consumer trans id key -> " + CommonConstants.SUB_TRANS_ID_KEY + "_" + transId.split("-")[1]));
        invocation.setAttachment(CommonConstants.SUB_TRANS_ID_KEY + "_" + transId.split("-")[1],
                transId);
    }

    private IObjectSerializerService getObjectSerializerService() {
        if (this.objectSerializerService != null) {
            return this.objectSerializerService;
        }
        synchronized (this) {
            if (this.objectSerializerService == null) {
                this.objectSerializerService = BeanUtil.getSpringCtx().getBean(IObjectSerializerService.class);
            }
        }
        return this.objectSerializerService;
    }

    private ITransactionInfoService getTransactionInfoService() {
        if (this.transactionInfoService != null) {
            return this.transactionInfoService;
        }
        synchronized (this) {
            if (this.transactionInfoService == null) {
                this.transactionInfoService = BeanUtil.getSpringCtx().getBean(ITransactionInfoService.class);
            }
        }
        return this.transactionInfoService;
    }

}
