package io.harness.event.client;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.harness.event.PublishMessage;

import java.util.Collections;
import java.util.Map;

public abstract class EventPublisher {
  abstract void publish(PublishMessage publishMessage);

  public final void publishMessage(Message message) {
    publishMessageWithAttributes(message, Collections.emptyMap());
  }

  public final void publishMessageWithAttributes(Message message, Map<String, String> attributes) {
    checkArgument(!(message instanceof PublishMessage)); // to avoid accidental nesting
    publish(PublishMessage.newBuilder().setPayload(Any.pack(message)).putAllAttributes(attributes).build());
  }

  public void shutdown() {}
}
