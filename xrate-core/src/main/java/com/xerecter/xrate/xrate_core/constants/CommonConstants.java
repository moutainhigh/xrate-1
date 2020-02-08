package com.xerecter.xrate.xrate_core.constants;

/**
 * @author xdd
 */
public class CommonConstants {

    /**
     * java序列化方式
     */
    public static String JAVA_SERIALIZER_WAY = "java";

    /**
     * kyro序列化方式
     */
    public static String KYRO_SERIALIZER_WAY = "kyro";

    /**
     * 项目名称前缀
     */
    public static String PRO_NAME_PREFIX = "xrate_";

    /**
     * mongodb序列化方式
     */
    public static String MONGODB_PERSISTENCE_WAY = "mongodb";

    /**
     * mysql序列化方式
     */
//    public static String MYSQL_PERSISTENCE_WAY = "mysql";

    /**
     * redis序列化方式
     */
//    public static String REDIS_PERSISTENCE_WAY = "redis";

    /**
     * file序列化方式
     */
//    public static String FILE_PERSISTENCE_WAY = "file";

    /**
     * 事务初始化状态，代表需要执行try方法
     */
    public static int TRANS_INIT_STATUS = 0;

    /**
     * 事务完成状态，代表try方法已经成功执行
     */
    public static int TRANS_SUCCESS_STATUS = 1;

    /**
     * 事务取消状态，代表需要执行成员的cancel方法
     */
    public static int TRANS_CANCEL_STATUS = -1;

    /**
     * 事务失败状态，代表此事务中所有事务成员cancel方法已经成功执行
     */
    public static int TRANS_FAIL_STATUS = -2;

    /**
     * 初始化是否为事务开启方
     */
    public static int INIT_START_SIDE = 0;

    /**
     * 代表是事务开启方
     */
    public static int IS_START_SIDE = 1;

    /**
     * 代表不是事务开启方
     */
    public static int NOT_START_SIDE = -1;

    /**
     * 已经生成的事务id key
     */
    public static String SUB_TRANS_ID_KEY = "xrate_sub_trans_id";

    /**
     * 期待执行的方法key
     */
    public static String AWAIT_EXECUTE_METHOD_KEY = "need_execute_method";

    /**
     * 期待执行try方法
     */
    public static String AWAIT_EXECUTE_TRY_METHOD = "try";

    /**
     * 期待执行cancel方法
     */
    public static String AWAIT_EXECUTE_CANCEL_METHOD = "cancel";

    /**
     * 期待执行success方法
     */
    public static String AWAIT_EXECUTE_SUCCESS_METHOD = "success";

    /**
     * 事务位置
     */
    public static String TRANS_POSITION_KEY = "xrate_trans_position";

}
