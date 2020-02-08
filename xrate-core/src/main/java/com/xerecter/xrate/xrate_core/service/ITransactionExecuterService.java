package com.xerecter.xrate.xrate_core.service;

import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 事务执行者服务接口
 *
 * @author xdd
 */
public interface ITransactionExecuterService {

    /**
     * 执行一个新的事务
     *
     * @param point 切点
     * @return 切点返回值
     */
    public Object executeNewTransaction(ProceedingJoinPoint point);

    /**
     * 执行一个已经开始的事务
     *
     * @param point 切点
     * @return 切点返回值
     */
    public Object executeStartedTransaction(ProceedingJoinPoint point);

    /**
     * 初始化一个事务
     *
     * @param point 切点
     * @return 初始化成功的事务信息
     */
    public TransactionInfo initTransaction(ProceedingJoinPoint point);

    /**
     * 初始化一个已经开始的事务
     *
     * @param point 切点
     * @return 初始化成功的事务信息
     */
    public TransactionInfo initStartedTransaction(ProceedingJoinPoint point);

    /**
     * 执行开始方取消事务操作
     *
     * @param transactionInfo 事务信息
     */
    public void executeStartSideCancelTransaction(TransactionInfo transactionInfo);

    /**
     * 执行已开始方取消事务操作
     *
     * @param transactionInfo 事务信息
     */
    public void executeStartedSideCancelTransaction(TransactionInfo transactionInfo);

    /**
     * 执行开始方的事务完成操作 这里仅仅只是删除对应的事务信息
     *
     * @param transactionInfo 事务信息
     */
    public void executeStartSideSuccessTransaction(TransactionInfo transactionInfo);

    /**
     * 执行已经开始方的事务完成操作 这里仅仅只是删除对应的事务信息
     *
     * @param transactionInfo 事务信息
     */
    public void executeStartedSideSuccessTransaction(TransactionInfo transactionInfo);

    /**
     * 移除事务信息和其成员信息
     *
     * @param transactionInfo 事务信息
     */
    public void removeTransactionAndMembers(TransactionInfo transactionInfo);

}
