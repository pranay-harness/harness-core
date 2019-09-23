package software.wings.service.impl.security;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

/**
 * @author marklu on 9/19/19
 */
public class SecretManagementException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public SecretManagementException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message, null);
  }

  public SecretManagementException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    super(null, null, errorCode, Level.ERROR, reportTargets, null);
  }

  public SecretManagementException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    super(null, null, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public SecretManagementException(ErrorCode errorCode, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
  }

  public SecretManagementException(
      ErrorCode errorCode, String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}