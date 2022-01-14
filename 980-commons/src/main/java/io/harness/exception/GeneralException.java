package io.harness.exception;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

@SuppressWarnings("squid:CallToDeprecatedMethod")
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

  public GeneralException(String message, Throwable throwable) {
    super(message, throwable, GENERAL_ERROR, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public GeneralException(String message, Throwable throwable, EnumSet<ReportTarget> reportTarget) {
    super(message, throwable, GENERAL_ERROR, Level.ERROR, reportTarget, null);
    param(MESSAGE_KEY, message);
  }
}
