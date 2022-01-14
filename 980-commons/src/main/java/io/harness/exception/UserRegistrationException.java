package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class UserRegistrationException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UserRegistrationException(String message, ErrorCode errorCode, EnumSet<ReportTarget> reportTarget) {
    super(message, null, errorCode, Level.ERROR, reportTarget, null);
    param(MESSAGE_KEY, message);
  }
}
