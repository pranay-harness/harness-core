package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class VerificationOperationException extends WingsException {
  public static final String REASON_KEY = "reason";

  public VerificationOperationException(ErrorCode errorCode, String reason) {
    super(null, null, errorCode, Level.ERROR, null, null);
    super.param(REASON_KEY, reason);
  }

  public VerificationOperationException(ErrorCode errorCode, String reason, Throwable cause) {
    super(null, cause, errorCode, Level.ERROR, null, null);
    super.param(REASON_KEY, reason);
  }

  public VerificationOperationException(ErrorCode errorCode, String reason, EnumSet<ReportTarget> reportTargets) {
    super(null, null, errorCode, Level.ERROR, reportTargets, null);
    super.param(REASON_KEY, reason);
  }

  public VerificationOperationException(
      ErrorCode errorCode, String reason, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
    super.param(REASON_KEY, reason);
  }
}
