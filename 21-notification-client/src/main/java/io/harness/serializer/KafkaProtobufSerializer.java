package io.harness.serializer;

import com.google.protobuf.MessageLite;

import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Serializer for Kafka to serialize Protocol Buffers messages
 *
 * @param <T> Protobuf message type
 */
public class KafkaProtobufSerializer<T extends MessageLite> implements Serializer<T> {
  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public byte[] serialize(String topic, T data) {
    return data.toByteArray();
  }

  @Override
  public void close() {}
}