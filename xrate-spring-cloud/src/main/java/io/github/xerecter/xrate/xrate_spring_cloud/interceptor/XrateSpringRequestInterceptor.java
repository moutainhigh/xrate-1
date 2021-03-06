package io.github.xerecter.xrate.xrate_spring_cloud.interceptor;

import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XrateSpringRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        TransactionUtil.printDebugInfo(() -> log.info(currTransactionInfo != null ? currTransactionInfo.toString() : "null trans"));
        if (currTransactionInfo != null) {
            XrateConfig xrateConfig;
            if (CommonConstants.IS_START_SIDE == TransactionUtil.getIsStartSide()) {
                xrateConfig = TransactionUtil.getCurrXrateConfig();
            } else if (CommonConstants.NOT_START_SIDE == TransactionUtil.getIsStartSide()) {
                xrateConfig = TransactionUtil.getCurrConnXrateConfig();
            } else {
                xrateConfig = null;
            }
            template.header(CommonConstants.ASYNC_INVOKE_KEY, String.valueOf(xrateConfig.getAsyncInvoke()));
            TransactionUtil.printDebugInfo(() -> log.info("consumer curr async -> " + xrateConfig.getAsyncInvoke()));
            template.header(CommonConstants.RETRY_TIMES_KEY, String.valueOf(xrateConfig.getRetryTimes()));
            TransactionUtil.printDebugInfo(() -> log.info("consumer curr retry times -> " + xrateConfig.getRetryTimes()));
            template.header(CommonConstants.RETRY_INTERVAL_KEY, String.valueOf(xrateConfig.getRetryInterval()));
            TransactionUtil.printDebugInfo(() -> log.info("consumer curr retry interval -> " + xrateConfig.getRetryInterval()));
            if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
                String parentTransId = currTransactionInfo.getTransId();
                int currMbPosition = TransactionUtil.incrCurrMbPosition();
                String subTransId = parentTransId.split("-")[0] + "-" + currMbPosition;
                TransactionUtil.printDebugInfo(() -> log.info("consumer provide trans id -> " + subTransId));
                TransactionUtil.printDebugInfo(() -> log.info("consumer provide position id -> " + currMbPosition));
                template.header(CommonConstants.AWAIT_EXECUTE_METHOD_KEY, CommonConstants.AWAIT_EXECUTE_TRY_METHOD);
                template.header(CommonConstants.SUB_TRANS_ID_KEY + "_" + currMbPosition, subTransId);
                template.header(CommonConstants.TRANS_POSITION_KEY + "_" + currMbPosition, String.valueOf(currMbPosition));
            } else if (CommonConstants.IS_START_SIDE == TransactionUtil.getIsStartSide() &&
                    CommonConstants.TRANS_CANCEL_STATUS == currTransactionInfo.getTransStatus()
            ) {
                onlySetSubTransId(template);
                template.header(CommonConstants.AWAIT_EXECUTE_METHOD_KEY, CommonConstants.AWAIT_EXECUTE_CANCEL_METHOD);
            } else if ((CommonConstants.NOT_START_SIDE == TransactionUtil.getIsStartSide() &&
                    currTransactionInfo.getNeedCancel())
            ) {
                onlySetSubTransId(template);
                template.header(CommonConstants.AWAIT_EXECUTE_METHOD_KEY, CommonConstants.AWAIT_EXECUTE_CANCEL_METHOD);
            } else if (CommonConstants.TRANS_SUCCESS_STATUS == currTransactionInfo.getTransStatus()) {
                onlySetSubTransId(template);
                template.header(CommonConstants.AWAIT_EXECUTE_METHOD_KEY, CommonConstants.AWAIT_EXECUTE_SUCCESS_METHOD);
            }
        }
    }

    private void onlySetSubTransId(RequestTemplate requestTemplate) {
        TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
        String transId = currTransMb.getTransId();
        TransactionUtil.printDebugInfo(() -> log.info("consumer trans id key -> " + CommonConstants.SUB_TRANS_ID_KEY + "_" + transId.split("-")[1]));
        requestTemplate.header(CommonConstants.SUB_TRANS_ID_KEY + "_" + transId.split("-")[1], transId);
    }

}
