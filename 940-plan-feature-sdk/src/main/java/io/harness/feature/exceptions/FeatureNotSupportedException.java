package io.harness.feature.exceptions;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class FeatureNotSupportedException extends WingsException {
  protected FeatureNotSupportedException(String message, Throwable cause) {
    super(message, cause, INVALID_REQUEST, Level.ERROR, USER_SRE, null);
  }
}
