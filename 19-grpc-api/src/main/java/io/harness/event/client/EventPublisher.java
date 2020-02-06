package io.harness.event.client;

import static com.google.common.base.Preconditions.checkArgument;
import static io.harness.grpc.IdentifierKeys.DELEGATE_ID;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import io.harness.data.structure.UUIDGenerator;
import io.harness.event.PublishMessage;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public abstract class EventPublisher {
  private final Supplier<String> delegateIdSupplier;

  protected EventPublisher(Supplier<String> delegateIdSupplier) {
    this.delegateIdSupplier = delegateIdSupplier;
  }

  protected abstract void publish(PublishMessage publishMessage);

  public void publishMessage(Message message, Timestamp occurredAt) {
    publishMessage(message, occurredAt, Collections.emptyMap());
  }

  public void publishMessage(Message message, Timestamp occurredAt, Map<String, String> attributes) {
    checkArgument(!(message instanceof PublishMessage)); // to avoid accidental nesting
    publish(PublishMessage.newBuilder()
                .setMessageId(UUIDGenerator.generateUuid())
                .setPayload(Any.pack(message))
                .setOccurredAt(occurredAt)
                .putAllAttributes(attributes)
                .putAttributes(DELEGATE_ID, delegateIdSupplier.get())
                .build());
  }

  public void shutdown() {}
}
