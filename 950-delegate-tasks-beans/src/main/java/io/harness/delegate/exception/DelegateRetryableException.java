/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.exception;

import static io.harness.eraro.ErrorCode.DELEGATE_TASK_RETRY;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class DelegateRetryableException extends WingsException {
  public DelegateRetryableException(Throwable cause) {
    super("Delegate retryable error.", cause, DELEGATE_TASK_RETRY, Level.ERROR, NOBODY, null);
  }

  public DelegateRetryableException(String message, Throwable cause) {
    super(message, cause, DELEGATE_TASK_RETRY, Level.ERROR, NOBODY, null);
  }
}
