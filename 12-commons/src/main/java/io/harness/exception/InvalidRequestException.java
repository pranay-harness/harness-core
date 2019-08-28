package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class InvalidRequestException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public InvalidRequestException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, null);
    super.param(MESSAGE_KEY, message);
  }

  public InvalidRequestException(String message, Throwable cause) {
    super(message, cause, INVALID_REQUEST, Level.ERROR, null);
    super.param(MESSAGE_KEY, message);
  }

  public InvalidRequestException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_REQUEST, Level.ERROR, reportTargets);
    super.param(MESSAGE_KEY, message);
  }

  public InvalidRequestException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, INVALID_REQUEST, Level.ERROR, reportTargets);
    super.param(MESSAGE_KEY, message);
  }
}
