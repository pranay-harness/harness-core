package io.harness.event.client;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import io.harness.event.PublishMessage;

import java.util.Collections;
import java.util.Map;

public abstract class EventPublisher {
  protected abstract void publish(PublishMessage publishMessage);

  public void publishMessage(Message message, Timestamp occurredAt) {
    publishMessage(message, occurredAt, Collections.emptyMap());
  }

  public void publishMessage(Message message, Timestamp occurredAt, Map<String, String> attributes) {
    checkArgument(!(message instanceof PublishMessage)); // to avoid accidental nesting
    publish(PublishMessage.newBuilder()
                .setPayload(Any.pack(message))
                .setOccurredAt(occurredAt)
                .putAllAttributes(attributes)
                .build());
  }

  public void shutdown() {}
}
