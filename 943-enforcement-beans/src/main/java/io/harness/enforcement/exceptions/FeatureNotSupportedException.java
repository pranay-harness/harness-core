package io.harness.enforcement.exceptions;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class FeatureNotSupportedException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public FeatureNotSupportedException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, USER_SRE, null);
    super.param(MESSAGE_ARG, message);
  }
}
