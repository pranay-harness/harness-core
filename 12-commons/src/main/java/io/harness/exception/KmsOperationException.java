package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import java.util.EnumSet;

public class KmsOperationException extends WingsException {
  private static final String REASON_KEY = "reason";

  public KmsOperationException(String reason) {
    super(INVALID_REQUEST);
    super.addParam(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, Throwable cause) {
    super(INVALID_REQUEST, cause);
    super.addParam(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, EnumSet<ReportTarget> reportTargets) {
    super(INVALID_REQUEST, reportTargets);
    super.addParam(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(INVALID_REQUEST, reportTargets, cause);
    super.addParam(REASON_KEY, reason);
  }
}
