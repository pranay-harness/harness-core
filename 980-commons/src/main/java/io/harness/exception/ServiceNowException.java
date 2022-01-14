package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class ServiceNowException extends WingsException {
  public ServiceNowException(String message, ErrorCode code, EnumSet<ReportTarget> reportTargets) {
    super(message, null, code, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }

  public ServiceNowException(String message, ErrorCode code, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, code, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
