package io.github.xerecter.xrate.xrate_core.service;

import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;

import java.util.List;

/**
 * 事务信息服务接口
 *
 * @author xdd
 */
public interface ITransactionInfoService {

    /**
     * 增加一个事务信息
     *
     * @param transactionInfo 事务信息
     * @return 增加成功的事务信息，失败返回null
     */
    public TransactionInfo addTransactionInfo(TransactionInfo transactionInfo);

    /**
     * 更新事务的状态
     *
     * @param transId 事务id
     * @param status  需要更新的事务内容
     * @return 是否更新成功
     */
    public boolean updateTransactionStatus(String transId, int status);

    /**
     * 更新事务的执行结果
     *
     * @param transId 事务id
     * @param result  结果
     * @return 是否更新成功
     */
    public boolean updateTransactionResult(String transId, byte[] result);

    /**
     * 更新事务的状态和执行结果
     *
     * @param transId 事务id
     * @param status  需要更新的事务内容
     * @param result  结果
     * @return 是否更新成功
     */
    public boolean updateTransactionStatusAndResult(String transId, int status, byte[] result);

    /**
     * 根据事务id和服务id获取事务信息
     *
     * @param transId   事务id
     * @param serviceId 服务id
     * @return 事务信息
     */
    public TransactionInfo getTransactionInfo(String transId, String serviceId);

    /**
     * 获取简单地事务信息 此结果不包括事务的返回值和参数值
     *
     * @param transId   事务id
     * @param serviceId 服务id
     * @return 事务信息
     */
    public TransactionInfo getSimpleTransactionInfo(String transId, String serviceId);

    /**
     * 获取简单地事务成员信息 此结果不包括事务的参数值
     *
     * @param parentTransId 父id
     * @return 成员信息
     */
    public List<TransactionMember> getSimpleTransactionMembers(String parentTransId);

    /**
     * 根据服务id获取事务信息
     *
     * @param serviceId 服务id
     * @return 事务信息
     */
    public List<TransactionInfo> getTransactionInfos(String serviceId);

    /**
     * 增加一个事务成员
     *
     * @param transactionMember 事务成员
     * @return 增加成功的事务成员，失败为null
     */
    public TransactionMember addTransactionMember(TransactionMember transactionMember);

    /**
     * 根据事务id获取事务成员
     *
     * @param parentTransId 事务id
     * @return 事务成员
     */
    public List<TransactionMember> getTransactionMembers(String parentTransId);

    /**
     * 删除事务信息
     *
     * @param transId 事务id
     * @return 是否删除成功
     */
    public boolean removeTransactionInfo(String transId);

    /**
     * 删除事务成员信息
     *
     * @param parentTransId 成员父id
     * @return 是否删除成功
     */
    public boolean removeTransactionMembers(String parentTransId);

    /**
     * 更新当前事务是否需要进行成员执行cancel方法
     *
     * @param needCancel 是否需要进行成员执行cancel方法
     * @return 是否更新成功
     */
    public boolean updateTransactionNeedCancel(String transId, boolean needCancel);

    /**
     * 更新当前事务是否需要进行成员执行cancel方法和事务状态
     *
     * @param needCancel 是否需要进行成员执行cancel方法
     * @param status     状态
     * @return 是否更新成功
     */
    public boolean updateTransactionNeedCancelAndStatus(String transId, boolean needCancel, int status);

    /**
     * 更新事务是否需要执行完成操作
     *
     * @param transId     事务id
     * @param needSuccess 是否需要执行完成操作
     * @return 是否更新成功
     */
    public boolean updateTransactionNeedSuccess(String transId, boolean needSuccess);

    /**
     * 更新事务是否需要执行完成操作和状态
     *
     * @param transId     事务id
     * @param needSuccess 是否需要执行完成操作
     * @param status      状态
     * @return 是否更新成功
     */
    public boolean updateTransactionNeedSuccessAndStatus(String transId, boolean needSuccess, int status);

}
