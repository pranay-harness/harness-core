package io.harness.event.client;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.harness.event.PublishMessage;

import java.util.Collections;
import java.util.Map;

public interface EventPublisher {
  void publish(PublishMessage publishMessage);

  default void publishMessage(Message message) {
    publishMessageWithAttributes(message, Collections.emptyMap());
  }

  default void publishMessageWithAttributes(Message message, Map<String, String> attributes) {
    checkArgument(!(message instanceof PublishMessage)); // call publish() directly.
    publish(PublishMessage.newBuilder().setPayload(Any.pack(message)).putAllAttributes(attributes).build());
  }

  void shutdown();
}
