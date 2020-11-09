package io.harness.exception;

import static io.harness.eraro.ErrorCode.ACCESS_DENIED;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class AccessDeniedException extends WingsException {
  public AccessDeniedException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, ACCESS_DENIED, Level.ERROR, reportTarget, null);
  }
}
