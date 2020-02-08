package com.xerecter.xrate.xrate_core.service.impl;

import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.xerecter.xrate.xrate_core.service.IObjectSerializerService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class JavaObjectSerializerImpl implements IObjectSerializerService {

    @Override
    public byte[] serializerObject(Object object) {
        int bufferSize = BUFFER_SIZE.intValue();
        byte[] bytes = null;
        boolean overflow = false;
        do {
            try (ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(bufferSize);
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteBufferOutputStream);) {
                objectOutputStream.writeObject(object);
                bytes = byteBufferOutputStream.getByteBuffer().array();
                overflow = false;
            } catch (Exception e) {
                if (e instanceof BufferOverflowException) {
                    overflow = true;
                    bufferSize *= 2;
                } else {
                    e.printStackTrace();
                }
            }
        } while (overflow);
        if (bufferSize > BUFFER_SIZE.intValue()) {
            BUFFER_SIZE.accumulate(bufferSize);
        }
        return bytes;
    }

    @Override
    public Object deserializerObject(byte[] bytes) {
        Object object = null;
        try (ByteBufferInputStream byteBufferInputStream = new ByteBufferInputStream(ByteBuffer.wrap(bytes));
             ObjectInputStream objectInputStream = new ObjectInputStream(byteBufferInputStream);) {
            object = objectInputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }


}
