package io.harness.beans.serializer;

public interface ProtobufSerializer<T> {
  String serialize(T object);
}
