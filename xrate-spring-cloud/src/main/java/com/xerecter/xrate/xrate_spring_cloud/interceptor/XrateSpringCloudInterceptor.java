package com.xerecter.xrate.xrate_spring_cloud.interceptor;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class XrateSpringCloudInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String awaitMethod = request.getHeader(CommonConstants.AWAIT_EXECUTE_METHOD_KEY);
        TransactionUtil.printDebugInfo(() -> log.info(" awaitMethod -> " + awaitMethod));
        if (CommonConstants.AWAIT_EXECUTE_TRY_METHOD.equalsIgnoreCase(awaitMethod)) {
            TransactionUtil.printDebugInfo(() -> log.info(" into -> " + awaitMethod));
            initTranactionInfo();
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            List<String> idKeys = new ArrayList<>();
            List<String> positionKeys = new ArrayList<>();
            Iterator<String> headerIterator = request.getHeaderNames().asIterator();
            while (headerIterator.hasNext()) {
                String key = headerIterator.next();
                if (key.toLowerCase().startsWith(CommonConstants.SUB_TRANS_ID_KEY)) {
                    idKeys.add(key);
                } else if (key.toLowerCase().startsWith(CommonConstants.TRANS_POSITION_KEY)) {
                    positionKeys.add(key);
                }
            }
            idKeys = idKeys.stream().sorted().collect(Collectors.toList());
            positionKeys = positionKeys.stream().sorted().collect(Collectors.toList());
            String transId = request.getHeader(idKeys.get(idKeys.size() - 1));
            List<String> finalIdKeys = idKeys;
            TransactionUtil.printDebugInfo(() -> log.info("provider trans id key -> " + finalIdKeys.get(finalIdKeys.size() - 1)));
            int position = Integer.parseInt(request.getHeader(positionKeys.get(positionKeys.size() - 1)));
            List<String> finalPositionKeys = positionKeys;
            TransactionUtil.printDebugInfo(() -> log.info("provider position id key -> " + finalPositionKeys.get(finalPositionKeys.size() - 1)));
            TransactionUtil.printDebugInfo(() -> log.info("provider id -> " + transId));
            TransactionUtil.printDebugInfo(() -> log.info("provider position -> " + position));
            TransactionUtil.setCurrMbPosition(position);
            currTransactionInfo.setTransId(transId);
            currTransactionInfo.setTransStatus(CommonConstants.TRANS_INIT_STATUS);
            TransactionUtil.setCurrHttpServletResponse(response);
        } else if (CommonConstants.AWAIT_EXECUTE_CANCEL_METHOD.equalsIgnoreCase(awaitMethod)) {
            TransactionUtil.printDebugInfo(() -> log.info(" into -> " + awaitMethod));
            initTranactionInfo();
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            String transId = onlyGetSubTransactionId(request);
            TransactionUtil.printDebugInfo(() -> log.info(awaitMethod + " get trans id -> " + transId));
            currTransactionInfo.setTransId(transId);
            currTransactionInfo.setTransStatus(CommonConstants.TRANS_CANCEL_STATUS);
        } else if (CommonConstants.AWAIT_EXECUTE_SUCCESS_METHOD.equalsIgnoreCase(awaitMethod)) {
            initTranactionInfo();
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            String transId = onlyGetSubTransactionId(request);
            TransactionUtil.printDebugInfo(() -> log.info(awaitMethod + " get trans id -> " + transId));
            currTransactionInfo.setTransId(transId);
            currTransactionInfo.setTransStatus(CommonConstants.TRANS_SUCCESS_STATUS);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TransactionUtil.removeAll();
    }

    private String onlyGetSubTransactionId(HttpServletRequest request) {
        List<String> idKeys = new ArrayList<>();
        Iterator<String> headerIterator = request.getHeaderNames().asIterator();
        while (headerIterator.hasNext()) {
            String key = headerIterator.next();
            if (key.toLowerCase().startsWith(CommonConstants.SUB_TRANS_ID_KEY)) {
                idKeys.add(key);
            }
        }
        idKeys = idKeys.stream().sorted().collect(Collectors.toList());
        return request.getHeader(idKeys.get(idKeys.size() - 1));
    }

    private void initTranactionInfo() {
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        if (currTransactionInfo == null) {
            currTransactionInfo = new TransactionInfo();
            TransactionUtil.setCurrTransactionInfo(currTransactionInfo);
        }
        if (TransactionUtil.getIsStartSide() == CommonConstants.INIT_START_SIDE) {
            TransactionUtil.setIsStartSide(CommonConstants.NOT_START_SIDE);
        }
    }
}
