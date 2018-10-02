package io.harness.exception;

import static io.harness.eraro.ErrorCode.KMS_OPERATION_ERROR;

import java.util.EnumSet;

public class KmsOperationException extends WingsException {
  private static final String REASON_KEY = "reason";

  public KmsOperationException(String reason) {
    super(KMS_OPERATION_ERROR);
    super.addParam(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, Throwable cause) {
    super(KMS_OPERATION_ERROR, cause);
    super.addParam(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, EnumSet<ReportTarget> reportTargets) {
    super(KMS_OPERATION_ERROR, reportTargets);
    super.addParam(REASON_KEY, reason);
  }

  public KmsOperationException(String reason, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(KMS_OPERATION_ERROR, reportTargets, cause);
    super.addParam(REASON_KEY, reason);
  }
}
