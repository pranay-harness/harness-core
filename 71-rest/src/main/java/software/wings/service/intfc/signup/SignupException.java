package software.wings.service.intfc.signup;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.FailureType;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class SignupException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public SignupException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, USER, null);
    super.param(MESSAGE_ARG, message);
  }

  public SignupException(String message, Throwable cause, ErrorCode code, Level level,
      EnumSet<ReportTarget> reportTargets, EnumSet<FailureType> failureTypes) {
    super(message, cause, code, level, reportTargets, failureTypes);
    param(MESSAGE_ARG, message);
  }
}
