package io.harness.distribution.idempotence;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

public class UnableToRegisterIdempotentOperationException extends WingsException {
  public UnableToRegisterIdempotentOperationException(String message) {
    super(ErrorCode.UNKNOWN_ERROR, message);
  }

  public UnableToRegisterIdempotentOperationException(Exception cause) {
    super(ErrorCode.UNKNOWN_ERROR, cause);
  }
}
