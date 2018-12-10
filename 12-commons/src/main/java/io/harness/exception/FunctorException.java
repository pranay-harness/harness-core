package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;
import java.util.Map;

public class FunctorException extends WingsException {
  public FunctorException(
      String message, Throwable cause, ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, code, level, reportTargets);
  }

  public FunctorException(String message) {
    super(message);
  }

  public FunctorException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, reportTargets);
  }

  public FunctorException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, reportTargets);
  }

  public FunctorException(String message, Throwable cause) {
    super(message, cause);
  }

  public FunctorException(Throwable cause) {
    super(cause);
  }

  public FunctorException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public FunctorException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    super(errorCode, message, reportTargets);
  }

  public FunctorException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(errorCode, reportTargets, cause);
  }

  public FunctorException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(errorCode, message, reportTargets, cause);
  }

  public FunctorException(ErrorCode errorCode) {
    super(errorCode);
  }

  public FunctorException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    super(errorCode, reportTargets);
  }

  public FunctorException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }

  public FunctorException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  public FunctorException(Map<String, Object> params, ErrorCode errorCode) {
    super(params, errorCode);
  }
}
