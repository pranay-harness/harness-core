package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

/**
 * @author marklu on 9/17/19
 */
public class SecretManagementDelegateException extends WingsException {
  private static final String REASON_KEY = "reason";

  public SecretManagementDelegateException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, errorCode, Level.ERROR, reportTargets, null);
    param(REASON_KEY, message);
  }

  public SecretManagementDelegateException(
      ErrorCode errorCode, String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, errorCode, Level.ERROR, reportTargets, null);
    param(REASON_KEY, message);
  }
}
