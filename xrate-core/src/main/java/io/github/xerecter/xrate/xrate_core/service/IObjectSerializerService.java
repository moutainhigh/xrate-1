package io.github.xerecter.xrate.xrate_core.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * 对象序列化接口
 *
 * @author xdd
 */
public interface IObjectSerializerService {

    /**
     * 缓冲大小
     */
    LongAccumulator BUFFER_SIZE = new LongAccumulator((oldVal, newVal) -> Math.max(newVal, oldVal), 32);

    /**
     * 序列化对象
     *
     * @param object 对象
     * @return 序列化后的字节
     */
    public byte[] serializerObject(Object object);

    /**
     * 反序列化对象
     *
     * @param bytes 字节
     * @return 对象
     */
    public Object deserializerObject(byte[] bytes);
}
