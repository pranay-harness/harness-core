package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_TOKEN;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class InvalidTokenException extends WingsException {
  public InvalidTokenException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_TOKEN, Level.ERROR, reportTargets, null);
  }
}
