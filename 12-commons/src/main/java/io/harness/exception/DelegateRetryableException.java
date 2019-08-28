package io.harness.exception;

import static io.harness.eraro.ErrorCode.DELEGATE_TASK_RETRY;

import io.harness.eraro.Level;

public class DelegateRetryableException extends WingsException {
  public DelegateRetryableException(Throwable cause) {
    super("Delegate retryable error.", cause, DELEGATE_TASK_RETRY, Level.ERROR, NOBODY);
  }

  public DelegateRetryableException(String message, Throwable cause) {
    super(message, cause, DELEGATE_TASK_RETRY, Level.ERROR, NOBODY);
  }
}
