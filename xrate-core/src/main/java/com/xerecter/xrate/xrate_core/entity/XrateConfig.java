package com.xerecter.xrate.xrate_core.entity;

import com.xerecter.xrate.xrate_core.constants.CommonConstants;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;

/**
 * 配置文件
 *
 * @author xdd
 */
@Data
@Accessors(chain = true)
public class XrateConfig {

    /**
     * 序列化方式
     */
    private String serializerWay = CommonConstants.KYRO_SERIALIZER_WAY;

    /**
     * 是否异步回调
     */
    private Boolean asyncInvoke = true;

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
    private Integer asyncBufferSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 重试cancel或者success方法的次数
     */
    private Integer retryTimes = 5;

    /**
     * 重试间隔 单位秒
     */
    private Integer retryInterval = 5;

    /**
     * 初始化检测间隔，也就是应用成功启动以后多长时间之后开始检测,单位秒
     */
    private Integer initCheckInterval = 0;

    /**
     * 是否需要初始化检测
     */
    private Boolean needInitCheck = true;

    /**
     * 是否开启调试，开启以后会打印出大量执行过程语句
     */
    private Boolean debugMode = false;

    @SneakyThrows
    @Override
    public Object clone() {
        XrateConfig xrateConfig = new XrateConfig();
        Field[] fields = xrateConfig.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object oldVal = field.get(this);
            field.set(xrateConfig, oldVal);
        }
        return xrateConfig;
    }
    
}
