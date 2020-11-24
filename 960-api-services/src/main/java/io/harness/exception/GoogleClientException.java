package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

public class GoogleClientException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public GoogleClientException(String message, ErrorCode errorCode) {
    super(message, null, errorCode, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public GoogleClientException(String message, ErrorCode errorCode, Throwable cause) {
    super(message, cause, errorCode, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }
}
