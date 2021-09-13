package io.harness.feature.exceptions;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class LimitExceededException extends WingsException {
  public LimitExceededException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, USER_SRE, null);
  }
}
