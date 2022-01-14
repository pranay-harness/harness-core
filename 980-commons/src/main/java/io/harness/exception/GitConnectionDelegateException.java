package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class GitConnectionDelegateException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public GitConnectionDelegateException(
      ErrorCode errorCode, Throwable cause, String message, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
