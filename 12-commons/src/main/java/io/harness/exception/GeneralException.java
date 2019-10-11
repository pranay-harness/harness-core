package io.harness.exception;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class GeneralException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public GeneralException(String message) {
    super(message, null, GENERAL_ERROR, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public GeneralException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, GENERAL_ERROR, Level.ERROR, reportTarget, null);
    param(MESSAGE_KEY, message);
  }
}
