package com.xerecter.xrate.xrate_dubbo.load_balance;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.entity.TransactionMember;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.loadbalance.RandomLoadBalance;

import java.util.List;

@Slf4j
public class XrateDubboLoadBalance extends RandomLoadBalance {
    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        if (currTransactionInfo != null) {
            if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
                Invoker<T> invoker = super.select(invokers, url, invocation);
                URL invokerUrl = invoker.getUrl();
                TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
                currTransMb.setAddress(invokerUrl.getHost() + ":" + invokerUrl.getPort());
                return invoker;
            }
        }
        return super.select(invokers, url, invocation);
    }
}
