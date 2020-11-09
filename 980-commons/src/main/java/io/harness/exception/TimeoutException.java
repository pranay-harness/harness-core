package io.harness.exception;

import static io.harness.eraro.ErrorCode.REQUEST_TIMEOUT;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class TimeoutException extends WingsException {
  private static final String NAME_ARG = "name";

  public TimeoutException(String message, String name, EnumSet<ReportTarget> reportTargets) {
    super(message, null, REQUEST_TIMEOUT, Level.ERROR, reportTargets, null);
    super.param(NAME_ARG, name);
  }

  public TimeoutException(String message, String name, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, REQUEST_TIMEOUT, Level.ERROR, reportTargets, null);
    super.param(NAME_ARG, name);
  }
}
