package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

public class FileReadException extends WingsException {
  public static final String MESSAGE_KEY = "message";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public FileReadException(String message) {
    super(null, null, ErrorCode.GENERAL_ERROR, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public FileReadException(String message, Throwable cause) {
    super(null, cause, ErrorCode.GENERAL_ERROR, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }
}
