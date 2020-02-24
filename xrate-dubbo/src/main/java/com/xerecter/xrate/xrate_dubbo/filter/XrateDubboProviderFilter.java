package com.xerecter.xrate.xrate_dubbo.filter;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Activate(group = {org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE}, order = 0)
public class XrateDubboProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String awaitMethod = invocation.getAttachment(CommonConstants.AWAIT_EXECUTE_METHOD_KEY);
        if (CommonConstants.AWAIT_EXECUTE_TRY_METHOD.equalsIgnoreCase(awaitMethod)) {
            initTransactionInfo();
            setCurrAndCurrConnXrateConfig(invocation);
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            Set<String> totalKeys = invocation.getAttachments().keySet().stream().filter(key ->
                    key.toLowerCase().startsWith(CommonConstants.SUB_TRANS_ID_KEY) ||
                            key.toLowerCase().startsWith(CommonConstants.TRANS_POSITION_KEY))
                    .collect(Collectors.toSet());
            List<String> idKeys = totalKeys.stream().filter(key ->
                    key.toLowerCase().startsWith(CommonConstants.SUB_TRANS_ID_KEY))
                    .sorted()
                    .collect(Collectors.toList());
            List<String> positionKeys = totalKeys.stream().filter(key ->
                    key.toLowerCase().startsWith(CommonConstants.TRANS_POSITION_KEY))
                    .sorted()
                    .collect(Collectors.toList());
            String transId = invocation.getAttachments().get(idKeys.get(idKeys.size() - 1));
            TransactionUtil.printDebugInfo(() -> log.info("provider trans id key -> " + idKeys.get(idKeys.size() - 1)));
            int position = Integer.parseInt(invocation.getAttachments().get(positionKeys.get(positionKeys.size() - 1)));
            TransactionUtil.printDebugInfo(() -> log.info("provider position id key -> " + idKeys.get(positionKeys.size() - 1)));
            TransactionUtil.printDebugInfo(() -> log.info("provider id -> " + transId));
            TransactionUtil.printDebugInfo(() -> log.info("provider position -> " + position));
            TransactionUtil.setCurrMbPosition(position);
            currTransactionInfo.setTransId(transId);
            currTransactionInfo.setTransStatus(CommonConstants.TRANS_INIT_STATUS);
            Result result;
            try {
                result = invoker.invoke(invocation);
                result.setAttachment(CommonConstants.TRANS_POSITION_KEY + "_" + TransactionUtil.getCurrMbPosition(),
                        String.valueOf(TransactionUtil.getCurrMbPosition()));
            } catch (Exception e) {
                result = new AsyncRpcResult(invocation);
                result.setValue(null);
                result.setException(e);
                result.setAttachment(CommonConstants.TRANS_POSITION_KEY + "_" + TransactionUtil.getCurrMbPosition(),
                        String.valueOf(TransactionUtil.getCurrMbPosition()));
                throw new IllegalArgumentException(e);
            } finally {
                TransactionUtil.removeAll();
            }
            return result;

        } else if (CommonConstants.AWAIT_EXECUTE_CANCEL_METHOD.equalsIgnoreCase(awaitMethod)) {
            initTransactionInfo();
            setCurrAndCurrConnXrateConfig(invocation);
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            String transId = onlyGetSubTransactionId(invocation.getAttachments());
            currTransactionInfo.setTransId(transId);
            currTransactionInfo.setTransStatus(CommonConstants.TRANS_CANCEL_STATUS);
            Result result = null;
            try {
                result = invoker.invoke(invocation);
            } catch (RpcException e) {
                e.printStackTrace();
            } finally {
                TransactionUtil.removeAll();
            }
            return result;
        } else if (CommonConstants.AWAIT_EXECUTE_SUCCESS_METHOD.equalsIgnoreCase(awaitMethod)) {
            initTransactionInfo();
            setCurrAndCurrConnXrateConfig(invocation);
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            String transId = onlyGetSubTransactionId(invocation.getAttachments());
            currTransactionInfo.setTransId(transId);
            currTransactionInfo.setTransStatus(CommonConstants.TRANS_SUCCESS_STATUS);
            Result result = null;
            try {
                result = invoker.invoke(invocation);
            } catch (RpcException e) {
                e.printStackTrace();
            } finally {
                TransactionUtil.removeAll();
            }
            return result;
        } else {
            return invoker.invoke(invocation);
        }
    }

    private String onlyGetSubTransactionId(Map<String, String> attachments) {
        List<String> keys = attachments.keySet().stream().filter(key ->
                key.toLowerCase().startsWith(CommonConstants.SUB_TRANS_ID_KEY))
                .sorted()
                .collect(Collectors.toList());
        TransactionUtil.printDebugInfo(() -> log.info("provider trans id key -> " + keys.get(keys.size() - 1)));
        return attachments.get(keys.get(keys.size() - 1));
    }

    private void initTransactionInfo() {
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        if (currTransactionInfo == null) {
            currTransactionInfo = new TransactionInfo();
            TransactionUtil.setCurrTransactionInfo(currTransactionInfo);
        }
        if (TransactionUtil.getIsStartSide() == CommonConstants.INIT_START_SIDE) {
            TransactionUtil.setIsStartSide(CommonConstants.NOT_START_SIDE);
        }
    }

    /**
     * 设置当前线程和当前连接的配置
     *
     * @param invocation 调用信息
     */
    private void setCurrAndCurrConnXrateConfig(Invocation invocation) {
        XrateConfig currConnXrateConfig = TransactionUtil.getCurrConnXrateConfig();
        String asyncInvoke = invocation.getAttachment(CommonConstants.ASYNC_INVOKE_KEY);
        String retryTimes = invocation.getAttachment(CommonConstants.RETRY_TIMES_KEY);
        String retryInterval = invocation.getAttachment(CommonConstants.RETRY_INTERVAL_KEY);
        TransactionUtil.printDebugInfo(() -> log.info("provider curr async -> " + asyncInvoke));
        TransactionUtil.printDebugInfo(() -> log.info("provider curr retry times -> " + retryTimes));
        TransactionUtil.printDebugInfo(() -> log.info("provider curr retry interval -> " + retryInterval));
        currConnXrateConfig.setAsyncInvoke(Boolean.valueOf(asyncInvoke));
        currConnXrateConfig.setRetryTimes(Integer.valueOf(retryTimes));
        currConnXrateConfig.setRetryInterval(Integer.valueOf(retryInterval));
        TransactionUtil.setCurrConnXrateConfig(currConnXrateConfig);
        XrateConfig currXrateConfig = TransactionUtil.getCurrXrateConfig();
        currXrateConfig.setAsyncInvoke(currConnXrateConfig.getAsyncInvoke());
        currXrateConfig.setRetryTimes(currConnXrateConfig.getRetryTimes());
        currXrateConfig.setRetryInterval(currConnXrateConfig.getRetryInterval());
        TransactionUtil.setCurrXrateConfig(currXrateConfig);
    }

}
