package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class InvalidCredentialsException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public InvalidCredentialsException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_CREDENTIAL, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }

  public InvalidCredentialsException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, INVALID_CREDENTIAL, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }
}
