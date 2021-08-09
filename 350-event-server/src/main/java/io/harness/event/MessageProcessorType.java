package io.harness.event;

import io.harness.event.grpc.ExceptionMessageProcessor;
import io.harness.event.grpc.MessageProcessor;

public enum MessageProcessorType {
  EXCEPTION(ExceptionMessageProcessor.class);

  private final Class<? extends MessageProcessor> messageProcessorClass;

  MessageProcessorType(Class<? extends MessageProcessor> messageProcessorClass) {
    this.messageProcessorClass = messageProcessorClass;
  }

  public Class<? extends MessageProcessor> getMessageProcessorClass() {
    return messageProcessorClass;
  }
}
