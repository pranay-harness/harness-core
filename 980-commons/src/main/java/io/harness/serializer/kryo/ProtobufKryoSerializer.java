package io.harness.serializer.kryo;

import com.google.protobuf.Message;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class ProtobufKryoSerializer<P extends Message> extends Serializer<P> {
  @Override
  public void write(Kryo kryo, Output output, P p) {
    try {
      p.writeTo(output);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SneakyThrows
  @Override
  public P read(Kryo kryo, Input input, Class<P> aClass) {
    Method method = aClass.getDeclaredMethod("parseFrom", InputStream.class);
    method.setAccessible(true);
    return (P) method.invoke(null, input);
  }
}