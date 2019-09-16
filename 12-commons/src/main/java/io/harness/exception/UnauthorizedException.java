package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_TOKEN;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class UnauthorizedException extends WingsException {
  public UnauthorizedException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, INVALID_TOKEN, Level.ERROR, reportTarget, null);
  }
}
