/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
