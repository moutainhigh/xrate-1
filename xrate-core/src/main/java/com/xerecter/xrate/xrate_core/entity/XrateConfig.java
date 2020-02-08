package com.xerecter.xrate.xrate_core.entity;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import lombok.Data;

/**
 * 配置文件
 *
 * @author xdd
 */
public class XrateConfig {

    /**
     * 序列化方式
     */
    private String serializerWay = CommonConstants.KYRO_SERIALIZER_WAY;

    /**
     * 是否异步回调
     */
    private boolean asyncInvoke = true;

    /**
     * 持久化方式
     */
    private String persistenceWay = CommonConstants.MONGODB_PERSISTENCE_WAY;

    /**
     * 服务名称
     */
    private String serviceName = "";

    /**
     * 服务id
     */
    private String serviceId = "";

    /**
     * 持久化配置
     */
    private Object persistenceConfig = null;

    /**
     * 异步缓冲大小
     */
    private int asyncBufferSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 重试cancel或者success方法的次数
     */
    private int retryTimes = 5;

    /**
     * 重试间隔 单位秒
     */
    private int retryInterval = 5;

    /**
     * 初始化检测间隔，也就是应用成功启动以后多长时间之后开始检测,单位秒
     */
    private int initCheckInterval = 0;

    /**
     * 是否需要初始化检测
     */
    private boolean needInitCheck = true;

    /**
     * 是否开启调试，开启以后会打印出大量执行过程语句
     */
    private Boolean debugMode = false;

    public String getSerializerWay() {
        return this.serializerWay;
    }

    public void setSerializerWay(String serializerWay) {
        this.serializerWay = serializerWay;
    }

    public boolean getAsyncInvoke() {
        return this.asyncInvoke;
    }

    public void setAsyncInvoke(boolean asyncInvoke) {
        this.asyncInvoke = asyncInvoke;
    }

    public String getPersistenceWay() {
        return this.persistenceWay;
    }

    public void setPersistenceWay(String persistenceWay) {
        this.persistenceWay = persistenceWay;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Object getPersistenceConfig() {
        return this.persistenceConfig;
    }

    public void setPersistenceConfig(Object persistenceConfig) {
        this.persistenceConfig = persistenceConfig;
    }

    public int getAsyncBufferSize() {
        return this.asyncBufferSize;
    }

    public void setAsyncBufferSize(int asyncBufferSize) {
        this.asyncBufferSize = asyncBufferSize;
    }

    public int getRetryTimes() {
        return this.retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getRetryInterval() {
        return this.retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public int getInitCheckInterval() {
        return this.initCheckInterval;
    }

    public void setInitCheckInterval(int initCheckInterval) {
        this.initCheckInterval = initCheckInterval;
    }

    public boolean getNeedInitCheck() {
        return this.needInitCheck;
    }

    public void setNeedInitCheck(boolean needInitCheck) {
        this.needInitCheck = needInitCheck;
    }

    public Boolean getDebugMode() {
        return this.debugMode;
    }

    public void setDebugMode(Boolean debugMode) {
        this.debugMode = debugMode;
    }

    private static XrateConfig xrateConfigInstance = null;

    public static XrateConfig getXrateConfigInstance() {
        return xrateConfigInstance;
    }

    public static void setXrateConfigInstance(XrateConfig xrateConfigInstance) {
        if (XrateConfig.xrateConfigInstance == null) {
            XrateConfig.xrateConfigInstance = xrateConfigInstance;
        }
    }

}
