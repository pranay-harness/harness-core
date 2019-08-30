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
