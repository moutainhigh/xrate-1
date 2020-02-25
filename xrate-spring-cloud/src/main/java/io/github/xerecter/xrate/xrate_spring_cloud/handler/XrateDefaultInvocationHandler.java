package io.github.xerecter.xrate.xrate_spring_cloud.handler;

import io.github.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.service.IObjectSerializerService;
import io.github.xerecter.xrate.xrate_core.util.BeanUtil;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class XrateDefaultInvocationHandler implements InvocationHandler, Serializable {

    private InvocationHandler delegate;

    private static final long serialVersionUID = 5531744639992436476L;

    private IObjectSerializerService objectSerializerService;

    public InvocationHandler getDelegate() {
        return this.delegate;
    }

    public void setDelegate(InvocationHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        XrateTransaction xrateTransaction = method.getAnnotation(XrateTransaction.class);
        if (CommonConstants.INIT_START_SIDE != TransactionUtil.getIsStartSide() &&
                xrateTransaction != null) {
            TransactionUtil.printDebugInfo(() -> log.info("method is marked XrateTransaction"));
            IObjectSerializerService objectSerializerService = getObjectSerializerService();
            TransactionInfo currTransactionInfo = TransactionUtil.getCurrTransactionInfo();
            if (CommonConstants.TRANS_INIT_STATUS == currTransactionInfo.getTransStatus()) {
                TransactionUtil.printDebugInfo(() -> log.info("exe xrate init trans -> " + currTransactionInfo.getTransId()));
                TransactionMember currTransMb = new TransactionMember();
                TransactionUtil.setCurrTransMb(currTransMb);
                List<String> paramClassNames = new ArrayList<>(args.length);
                for (int i = 0; i < args.length; i++) {
                    paramClassNames.add(args[i].getClass().getName());
                }
                currTransMb.setParams(objectSerializerService.serializerObject(args));
                currTransMb.setParamClassNames(paramClassNames);
                currTransMb.setMemberClassName(method.getDeclaringClass().getName());
                currTransMb.setTryName(method.getName());
                Object result = delegate.invoke(proxy, method, args);
                TransactionUtil.printDebugInfo(() -> log.info(" remote execute success "));
                return result;
            }
            return delegate.invoke(proxy, method, args);
        } else {
            TransactionUtil.removeIsStartSide();
            return delegate.invoke(proxy, method, args);
        }
    }


    private IObjectSerializerService getObjectSerializerService() {
        if (objectSerializerService != null) {
            return objectSerializerService;
        }
        synchronized (this) {
            if (objectSerializerService == null) {
                objectSerializerService = BeanUtil.getSpringCtx().getBean(IObjectSerializerService.class);
            }
        }
        return objectSerializerService;
    }

}
