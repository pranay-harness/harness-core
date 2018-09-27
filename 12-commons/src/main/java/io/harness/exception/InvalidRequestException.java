package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import java.util.EnumSet;

public class InvalidRequestException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public InvalidRequestException(String message) {
    super(INVALID_REQUEST);
    super.addParam(MESSAGE_KEY, message);
  }

  public InvalidRequestException(String message, Throwable cause) {
    super(INVALID_REQUEST, cause);
    super.addParam(MESSAGE_KEY, message);
  }

  public InvalidRequestException(String message, EnumSet<ReportTarget> reportTargets) {
    super(INVALID_REQUEST, reportTargets);
    super.addParam(MESSAGE_KEY, message);
  }

  public InvalidRequestException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(INVALID_REQUEST, reportTargets, cause);
    super.addParam(MESSAGE_KEY, message);
  }
}
