/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.publisher;

import static io.harness.eraro.ErrorCode.EVENT_PUBLISH_FAILED;

import io.harness.exception.WingsException;

public class EventPublishException extends WingsException {
  public EventPublishException(Throwable cause) {
    super(EVENT_PUBLISH_FAILED, cause);
  }

  public EventPublishException(String message, Throwable e) {
    super(EVENT_PUBLISH_FAILED, message, e);
  }
}
