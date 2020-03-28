package io.harness.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class HelmClientException extends WingsException {
  public HelmClientException(String message) {
    super(message, null, DEFAULT_ERROR_CODE, Level.ERROR, null, null);
  }

  public HelmClientException(String message, Throwable cause) {
    super(message, cause, DEFAULT_ERROR_CODE, Level.ERROR, null, null);
  }

  public HelmClientException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, DEFAULT_ERROR_CODE, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }

  public HelmClientException(String message, EnumSet<ReportTarget> reportTargets, Throwable t) {
    super(message, t, DEFAULT_ERROR_CODE, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }
}
