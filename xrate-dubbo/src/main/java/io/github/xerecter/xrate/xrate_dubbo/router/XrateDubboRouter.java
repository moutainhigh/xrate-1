package io.github.xerecter.xrate.xrate_dubbo.router;

import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.util.ReflectUtil;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.router.condition.config.AppRouter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class XrateDubboRouter extends AppRouter {

    public XrateDubboRouter(DynamicConfiguration configuration, URL url) {
        super(configuration, url);
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        invokers.forEach((invoker) -> {
            try {
                URL invokerUrl = invoker.getUrl().addParameter(Constants.LOADBALANCE_KEY, "xrateDubboLoadBalance");
                ReflectUtil.setFieldValue(invoker, invokerUrl, "url");
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        if (currTransactionInfo != null) {
            if (CommonConstants.INIT_START_SIDE != TransactionUtil.getIsStartSide()) {
                if (CommonConstants.TRANS_CANCEL_STATUS == currTransactionInfo.getTransStatus() ||
                        CommonConstants.TRANS_SUCCESS_STATUS == currTransactionInfo.getTransStatus()) {
                    TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
                    return getTransactionMemberInvoker((List<Invoker<T>>) invokers, currTransMb);
                } else {
                    invokers = super.route(invokers, url, invocation);
                    int currMbPosition = TransactionUtil.getCurrMbPosition();
                    String nextTransId = currTransactionInfo.getTransId().split("-")[0] + "-" + (++currMbPosition);
                    if (TransactionUtil.processTransMbExistsMb(nextTransId)) {
                        TransactionMember currTransMb = TransactionUtil.getProcessTransMb().get(nextTransId);
                        TransactionUtil.setCurrTransMb(currTransMb);
                        return getTransactionMemberInvoker((List<Invoker<T>>) invokers, currTransMb);
                    } else {
                        if (invokers.size() == 1) {
                            TransactionMember currTransMb = new TransactionMember();
                            TransactionUtil.setCurrTransMb(currTransMb);
                            Invoker<T> invoker = invokers.get(0);
                            URL invokerUrl = invoker.getUrl();
                            currTransMb.setAddress(invokerUrl.getHost() + ":" + invokerUrl.getPort());
                        }
                    }
                    return invokers;
                }
            }
        }
        return super.route(invokers, url, invocation);
    }

    private <T> List<Invoker<T>> getTransactionMemberInvoker(List<Invoker<T>> invokers, TransactionMember currTransMb) {
        String address = currTransMb.getAddress();
        String[] splitAddress = address.split(":");
        String host = splitAddress[0];
        int port = Integer.parseInt(splitAddress[1]);
        Invoker<T> invoker = null;
        for (Invoker<T> invoker1 : invokers) {
            if (invoker1.getUrl().getHost().equals(host) && invoker1.getUrl().getPort() == port) {
                invoker = invoker1;
            }
        }
        TransactionUtil.printDebugInfo(() -> log.info("select address -> " + currTransMb.getTransId() + " -> " + currTransMb.getAddress()));
        Assert.isTrue(invoker != null, "no available invoker");
        List<Invoker<T>> invokerList = new ArrayList<>(1);
        invokerList.add(invoker);
        return invokerList;
    }
}
