package io.github.xerecter.xrate.xrate_core.util;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.dto.TransactionInfoDto;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * @author xdd
 */
public class TransactionUtil {
    /**
     * 当前事务信息
     */
    private static ThreadLocal<TransactionInfo> CURR_TRANSACTION = new ThreadLocal<>();

    private static boolean DEBUG_MODE = false;

    /**
     * 判断当前是否为开始方 0 代表未设置 1 代表是 -1 代表不是
     */
    private static ThreadLocal<Integer> IS_START_SIDE = ThreadLocal.withInitial(() -> CommonConstants.INIT_START_SIDE);

    /**
     * 当前事务成员位置
     */
    private static ThreadLocal<Integer> CURR_MB_POSITION = ThreadLocal.withInitial(() -> 0);

    /**
     * 当前正在处理中的事务成员
     */
    private static ThreadLocal<TransactionMember> CURR_TRANS_MB = new ThreadLocal<>();

    /**
     * 用于异步事务操作的disruptor
     */
    private static Disruptor<TransactionInfoDto> TRANS_DISRUPTOR = null;

    /**
     * 当前配置
     */
    private static ThreadLocal<XrateConfig> CURR_CONFIG = ThreadLocal.withInitial(() -> ((XrateConfig) BeanUtil.getSpringCtx().getBean(XrateConfig.class).clone()));

    /**
     * 当前连接的配置
     */
    private static ThreadLocal<XrateConfig> CURR_CONN_CONFIG = ThreadLocal.withInitial(() -> {
        XrateConfig xrateConfig = new XrateConfig();
        xrateConfig.setAsyncInvoke(null);
        xrateConfig.setRetryInterval(-1);
        xrateConfig.setRetryTimes(-1);
        return xrateConfig;
    });

    /**
     * 用于异步事务操作的线程池
     */
    private static ThreadPoolExecutor TRANS_EXECUTOR = null;

    /**
     * 已经处理的事务成员
     */
    private static ThreadLocal<HashMap<String, TransactionMember>> PROCESS_TRANS_MB = new ThreadLocal<>();

    /**
     * 为了适配spring cloud,记录当前response
     */
    private static ThreadLocal<HttpServletResponse> CURR_RESPONSE = new ThreadLocal<>();

    /**
     * 事务调度池，用于事务重试时使用
     */
    private static ScheduledExecutorService TRANS_SCHEDULED = null;

    /**
     * 获取当前配置
     *
     * @return 对应的信息
     */
    public static XrateConfig getCurrXrateConfig() {
        return CURR_CONFIG.get();
    }

    /**
     * 设置当前配置
     *
     * @param xrateConfig 新的配置
     * @return 对应的配置
     */
    public static XrateConfig setCurrXrateConfig(XrateConfig xrateConfig) {
        CURR_CONFIG.set(xrateConfig);
        return CURR_CONFIG.get();
    }

    /**
     * 移除当前配置
     */
    public static void removeCurrXrateConfig() {
        CURR_CONFIG.remove();
    }

    /**
     * 获取当前连接配置
     *
     * @return 对应的信息
     */
    public static XrateConfig getCurrConnXrateConfig() {
        return CURR_CONN_CONFIG.get();
    }

    /**
     * 设置当前连接配置
     *
     * @param xrateConfig 新的配置
     * @return 对应的配置
     */
    public static XrateConfig setCurrConnXrateConfig(XrateConfig xrateConfig) {
        CURR_CONN_CONFIG.set(xrateConfig);
        return CURR_CONN_CONFIG.get();
    }

    /**
     * 移除当前连接配置
     */
    public static void removeCurrConnXrateConfig() {
        CURR_CONN_CONFIG.remove();
    }

    /**
     * 获取当前事务信息
     *
     * @return 当前事务信息
     */
    public static TransactionInfo getCurrTransactionInfo() {
        return CURR_TRANSACTION.get();
    }

    /**
     * 设置当前事务信息
     */
    public static void setCurrTransactionInfo(TransactionInfo transactionInfo) {
        CURR_TRANSACTION.set(transactionInfo);
    }

    /**
     * 移除当前事务信息
     */
    public static void removeCurrTransactionInfo() {
        CURR_TRANSACTION.remove();
    }


    /**
     * 移除是否为开始方
     */
    public static void removeIsStartSide() {
        IS_START_SIDE.remove();
    }

    /**
     * 设置是否为开始方
     *
     * @param isStartSide 是否为开始方
     */
    public static void setIsStartSide(int isStartSide) {
        IS_START_SIDE.set(isStartSide);
    }

    /**
     * 返回是否为开始方
     *
     * @return 是否为开始方
     */
    public static int getIsStartSide() {
        return IS_START_SIDE.get();
    }

    /**
     * 获取当前成员位置
     *
     * @return 当前成员位置
     */
    public static int getCurrMbPosition() {
        return CURR_MB_POSITION.get();
    }

    /**
     * 设置当前成员位置
     *
     * @param position 当前成员位置
     */
    public static void setCurrMbPosition(int position) {
        CURR_MB_POSITION.set(position);
    }

    /**
     * 自增当前成员位置
     *
     * @return 自增后的当前成员位置
     */
    public static int incrCurrMbPosition() {
        int currMbPosition = getCurrMbPosition();
        CURR_MB_POSITION.set(++currMbPosition);
        return currMbPosition;
    }

    /**
     * 移除当前成员位置
     */
    public static void removeCurrMbPosition() {
        CURR_MB_POSITION.remove();
    }

    /**
     * 获取当前正在处理中的事务成员
     *
     * @return 事务成员
     */
    public static TransactionMember getCurrTransMb() {
        return CURR_TRANS_MB.get();
    }

    /**
     * 设置当前正在处理中的事务成员
     *
     * @param transactionMember 事务成员
     */
    public static void setCurrTransMb(TransactionMember transactionMember) {
        CURR_TRANS_MB.set(transactionMember);
    }

    /**
     * 移除当前正在处理中的事务成员
     */
    public static void removeCurrTransMb() {
        CURR_TRANS_MB.remove();
    }

    /**
     * 初始化用于异步事务操作的disruptor
     *
     * @param bufferSize 缓冲大小
     */
    public static void initTransactionDisruptor(
            int bufferSize
    ) {
        TRANS_DISRUPTOR = new Disruptor<>(TransactionInfoDto::new, bufferSize, (runnable) -> {
            Thread thread = new Thread(runnable);
            thread.setName("xrate-transaction-disruptor-thread");
            return thread;
        }, ProducerType.SINGLE, new YieldingWaitStrategy());
        TRANS_DISRUPTOR.handleEventsWith((transactionInfoDto, sequence, endOfBatch) -> {
            transactionInfoDto.getExecutor().execute(transactionInfoDto);
        });
        TRANS_DISRUPTOR.start();
    }

//    public static void initTransactionExecutor(int bufferSize) {
//        TRANS_EXECUTOR = new ThreadPoolExecutor(
//                bufferSize,
//                bufferSize * 2,
//                10,
//                TimeUnit.SECONDS,
//                new LinkedBlockingQueue<>(),
//                (runnable) -> {
//                    Thread thread = new Thread(runnable);
//                    thread.setName("Xrate-Executer");
//                    return thread;
//                }
//        );
//    }

    /**
     * 初始化调度线程池
     *
     * @param coreSize 线程池核心大小
     */
    public static void initTransactionScheduled(int coreSize) {
        TRANS_SCHEDULED = Executors.newScheduledThreadPool(coreSize, (runnable) -> {
            Thread thread = new Thread(runnable);
            thread.setName("xrate-transaction-scheduled-thread");
            return thread;
        });
    }

    /**
     * 获取调度线程池
     *
     * @return 调度线程池
     */
    public static ScheduledExecutorService getTransactionScheduled() {
        return TRANS_SCHEDULED;
    }

    /**
     * 获取用于异步事务操作的disruptor
     *
     * @return 用于异步事务操作的disruptor
     */
    public static Disruptor<TransactionInfoDto> getTransDisruptor() {
        return TRANS_DISRUPTOR;
    }

    public static ThreadPoolExecutor getTransExecutor() {
        return TRANS_EXECUTOR;
    }

    /**
     * 发布一个事务处理者到disruptor
     *
     * @param transactionInfo 事务信息
     * @param retryTimes      充实次数
     * @param executor        执行者
     */
    public static void publishAnTransactionProcessor(
            TransactionInfo transactionInfo,
            int retryTimes,
            boolean mainCancel,
            boolean memberCancel,
            TransactionInfoDto.Executor executor
    ) {
//        ThreadPoolExecutor transExecutor = getTransExecutor();
//        TransactionInfoDto transactionInfoDto = new TransactionInfoDto();
//        transactionInfoDto.setMainCancel(mainCancel);
//        transactionInfoDto.setMemberCancel(memberCancel);
//        transactionInfoDto.setRetryTimes(retryTimes);
//        transactionInfoDto.setTransactionInfo(transactionInfo);
//        transactionInfoDto.setExecutor(executor);
//        transExecutor.execute(() -> transactionInfoDto.getExecutor().execute(transactionInfoDto));
        Disruptor<TransactionInfoDto> transDisruptor = getTransDisruptor();
        RingBuffer<TransactionInfoDto> ringBuffer = transDisruptor.getRingBuffer();
        long next = -1;
        try {
            next = ringBuffer.next();
            TransactionInfoDto transactionInfoDto = ringBuffer.get(next);
            transactionInfoDto.setMainCancel(mainCancel);
            transactionInfoDto.setMemberCancel(memberCancel);
            transactionInfoDto.setRetryTimes(retryTimes);
            transactionInfoDto.setTransactionInfo(transactionInfo);
            transactionInfoDto.setExecutor(executor);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ringBuffer.publish(next);
        }
    }

    /**
     * 发布一个事务处理者到disruptor
     *
     * @param transactionInfo 事务信息
     * @param executor        执行者
     */
    public static void publishAnTransactionProcessor(TransactionInfo transactionInfo, TransactionInfoDto.Executor executor) {
        publishAnTransactionProcessor(transactionInfo, 0, false, false, executor);
    }

    /**
     * 判断已经处理的事务成员是否已经存在
     *
     * @param transId 事务id
     * @return 是否存在
     */
    public static boolean processTransMbExistsMb(String transId) {
        return PROCESS_TRANS_MB.get() != null && PROCESS_TRANS_MB.get().containsKey(transId);
    }

    /**
     * 设置已经处理的事务成员
     *
     * @param processTransMb 事务成员
     */
    public static void setProcessTransMb(HashMap<String, TransactionMember> processTransMb) {
        PROCESS_TRANS_MB.set(processTransMb);
    }

    /**
     * 获取已经处理的事务成员
     *
     * @return 已经处理的事务成员
     */
    public static HashMap<String, TransactionMember> getProcessTransMb() {
        return PROCESS_TRANS_MB.get();
    }

    public static HttpServletResponse getCurrHttpServletResponse() {
        return CURR_RESPONSE.get();
    }

    public static void setCurrHttpServletResponse(HttpServletResponse response) {
        CURR_RESPONSE.set(response);
    }

    public static void removeCurrHttpServletResponse() {
        CURR_RESPONSE.remove();
    }

    /**
     * 移除已经处理的事务成员
     */
    public static void removeProcessTransMb() {
        PROCESS_TRANS_MB.remove();
    }

    public static void removeAll() {
        removeCurrTransactionInfo();
        removeCurrTransMb();
        removeIsStartSide();
        removeCurrMbPosition();
        removeProcessTransMb();
        removeCurrHttpServletResponse();
        removeCurrXrateConfig();
    }

    /**
     * 设置是否为调试模式
     *
     * @param debugMode 是否为调试模式
     */
    public static void setDebugMode(boolean debugMode) {
        DEBUG_MODE = debugMode;
    }

    /**
     * 打印debug信息
     *
     * @param executor 执行对象
     */
    public static void printDebugInfo(Executor executor) {
        if (DEBUG_MODE) {
            executor.method();
        }
    }

    public static interface Executor {

        public void method();

    }

}
