package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class LimitsExceededException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public LimitsExceededException(String message, ErrorCode code, EnumSet<ReportTarget> reportTarget) {
    super(message, null, code, Level.ERROR, reportTarget, null);
    param(MESSAGE_KEY, message);
  }
}
