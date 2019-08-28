package io.harness.exception;

import static io.harness.eraro.ErrorCode.KMS_OPERATION_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class KmsOperationException extends WingsException {
  private static final String REASON_KEY = "reason";

  public KmsOperationException(String reason) {
    super(null, null, KMS_OPERATION_ERROR, Level.ERROR, null);
    super.param(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, Throwable cause) {
    super(null, cause, KMS_OPERATION_ERROR, Level.ERROR, null);
    super.param(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, EnumSet<ReportTarget> reportTargets) {
    super(null, null, KMS_OPERATION_ERROR, Level.ERROR, reportTargets);
    super.param(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, KMS_OPERATION_ERROR, Level.ERROR, reportTargets);
    super.param(REASON_KEY, reason);
  }
}
