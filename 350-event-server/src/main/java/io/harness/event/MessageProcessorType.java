/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
