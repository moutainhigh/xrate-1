package com.xerecter.xrate.xrate_core.service.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.xerecter.xrate.xrate_core.service.IObjectSerializerService;

import java.nio.ByteBuffer;

public class KryoObjectSerializerImpl implements IObjectSerializerService {

    private static KryoPool kryoPool = new KryoPool.Builder(Kryo::new).build();

    @Override
    public byte[] serializerObject(Object object) {
        Kryo kryo = kryoPool.borrow();
        byte[] bytes = null;
        boolean byteBufferOverflow = false;
        int bufferSize = KryoObjectSerializerImpl.BUFFER_SIZE.intValue();
        do {
            try (ByteBufferOutput byteBufferOutput = new ByteBufferOutput(bufferSize);) {
                kryo.writeClassAndObject(byteBufferOutput, object);
                byteBufferOutput.flush();
                bytes = byteBufferOutput.toBytes();
                byteBufferOverflow = false;
            } catch (Exception e) {
                if (e.getMessage().contains("overflow")) {
                    byteBufferOverflow = true;
                    bufferSize *= 2;
                } else {
                    e.printStackTrace();
                }
            } finally {
                kryoPool.release(kryo);
            }
        } while (byteBufferOverflow);
        if (bufferSize > KryoObjectSerializerImpl.BUFFER_SIZE.intValue()) {
            KryoObjectSerializerImpl.BUFFER_SIZE.accumulate(bufferSize);
        }
        return bytes;
    }

    @Override
    public Object deserializerObject(byte[] bytes) {
        Kryo kryo = kryoPool.borrow();
        Object value = null;
        try (ByteBufferInput bufferInput = new ByteBufferInput(ByteBuffer.wrap(bytes));) {
            Object object = kryo.readClassAndObject(bufferInput);
            value = object;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            kryoPool.release(kryo);
        }
        return value;
    }
}
