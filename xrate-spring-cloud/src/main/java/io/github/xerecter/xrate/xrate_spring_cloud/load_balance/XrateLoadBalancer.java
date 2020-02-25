package io.github.xerecter.xrate.xrate_spring_cloud.load_balance;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import io.github.xerecter.xrate.xrate_core.util.BeanUtil;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class XrateLoadBalancer implements ILoadBalancer {

    private ILoadBalancer delegate;

    private ITransactionInfoService transactionInfoService = null;

    public XrateLoadBalancer(ILoadBalancer loadBalancer) {
        this.delegate = loadBalancer;
    }

    @Override
    public void addServers(List<Server> newServers) {
        delegate.addServers(newServers);
    }

    @Override
    public Server chooseServer(Object key) {
        TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
        if (currTransactionInfo != null) {
            if (CommonConstants.TRANS_CANCEL_STATUS == currTransactionInfo.getTransStatus() ||
                    CommonConstants.TRANS_SUCCESS_STATUS == currTransactionInfo.getTransStatus()) {
                TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
                return getTransMbServer(delegate.getAllServers(), currTransMb);
            } else {
                int currMbPosition = TransactionUtil.getCurrMbPosition();
                String nextTransId = currTransactionInfo.getTransId().split("-")[0] + "-" + (currMbPosition);
                if (TransactionUtil.processTransMbExistsMb(nextTransId)) {
                    TransactionMember currTransMb = TransactionUtil.getProcessTransMb().get(nextTransId);
                    TransactionUtil.setCurrTransMb(currTransMb);
                    return getTransMbServer(delegate.getAllServers(), currTransMb);
                } else {
                    ITransactionInfoService transactionInfoService = getTransactionInfoService();
                    TransactionMember currTransMb = TransactionUtil.getCurrTransMb();
                    Server server = delegate.chooseServer(key);
                    TransactionUtil.printDebugInfo(() -> log.info("select new server -> " + server.getHostPort()));
                    currTransMb.setParentTransId(currTransactionInfo.getTransId());
                    currTransMb.setAddress(server.getHost() + ":" + server.getPort());
                    currTransMb.setTransId(nextTransId);
                    currTransMb = transactionInfoService.addTransactionMember(currTransMb);
                    currTransactionInfo.getTransactionMembers().add(currTransMb);
                    return server;
                }
            }
        }
        return delegate.chooseServer(key);
    }

    @Override
    public void markServerDown(Server server) {
        delegate.markServerDown(server);
    }

    @Override
    public List<Server> getServerList(boolean availableOnly) {
        return delegate.getServerList(availableOnly);
    }

    @Override
    public List<Server> getReachableServers() {
        return delegate.getReachableServers();
    }

    @Override
    public List<Server> getAllServers() {
        return delegate.getAllServers();
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

    private Server getTransMbServer(List<Server> servers, TransactionMember transactionMember) {
        String[] address = transactionMember.getAddress().split(":");
        String host = address[0];
        int port = Integer.parseInt(address[1]);
        for (Server server : servers) {
            if (server.getHost().equals(host) && server.getPort() == port) {
                TransactionUtil.printDebugInfo(() -> log.info("select server -> " + server.getHostPort()));
                return server;
            }
        }
        throw new IllegalArgumentException("no available server");
    }

}
