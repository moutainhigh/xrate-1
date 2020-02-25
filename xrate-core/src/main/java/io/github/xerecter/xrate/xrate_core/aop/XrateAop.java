package io.github.xerecter.xrate.xrate_core.aop;

import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.service.ITransactionExecuterService;
import io.github.xerecter.xrate.xrate_core.util.TransactionUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Aspect
@Configuration
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class XrateAop {

    @Autowired
    ITransactionExecuterService transactionExecuteService;

    @Pointcut(value = "@annotation(io.github.xerecter.xrate.xrate_core.annotation.XrateTransaction)")
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object proxyMethod(ProceedingJoinPoint point) throws Throwable {
        if (TransactionUtil.getIsStartSide() == CommonConstants.INIT_START_SIDE) {
            return transactionExecuteService.executeNewTransaction(point);
        } else if (TransactionUtil.getIsStartSide() == CommonConstants.NOT_START_SIDE) {
            return transactionExecuteService.executeStartedTransaction(point);
        }
        return point.proceed();
    }

}
