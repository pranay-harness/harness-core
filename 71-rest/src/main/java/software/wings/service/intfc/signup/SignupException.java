package software.wings.service.intfc.signup;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class SignupException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public SignupException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, USER, null);
    super.param(MESSAGE_ARG, message);
  }
}
