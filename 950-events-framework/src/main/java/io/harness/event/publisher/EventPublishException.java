/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
