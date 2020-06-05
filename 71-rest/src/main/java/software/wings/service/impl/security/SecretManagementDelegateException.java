package software.wings.service.impl.security;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

/**
 * @author marklu on 9/17/19
 */
@SuppressWarnings("squid:CallToDeprecatedMethod")
public class SecretManagementDelegateException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public SecretManagementDelegateException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public SecretManagementDelegateException(
      ErrorCode errorCode, String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
