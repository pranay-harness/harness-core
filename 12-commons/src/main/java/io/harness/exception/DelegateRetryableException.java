package io.harness.exception;

import static io.harness.eraro.ErrorCode.DELEGATE_TASK_RETRY;

public class DelegateRetryableException extends WingsException {
  public DelegateRetryableException(Throwable cause) {
    super(DELEGATE_TASK_RETRY, "Delegate retryable error.", NOBODY, cause);
  }

  public DelegateRetryableException(String message, Throwable cause) {
    super(DELEGATE_TASK_RETRY, message, NOBODY, cause);
  }
}
