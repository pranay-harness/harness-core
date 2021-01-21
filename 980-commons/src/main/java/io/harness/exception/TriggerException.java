package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class TriggerException extends WingsException {
  public TriggerException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_REQUEST, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }

  public TriggerException(String message, Throwable throwable, EnumSet<ReportTarget> reportTargets) {
    super(message, throwable, INVALID_REQUEST, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }

  public TriggerException(ErrorCode errorCode) {
    super(null, null, errorCode, Level.ERROR, null, null);
  }
}
