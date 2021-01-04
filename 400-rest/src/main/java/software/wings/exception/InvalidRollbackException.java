package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InvalidRollbackException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public InvalidRollbackException(String details, ErrorCode errorCode) {
    super(null, null, errorCode, Level.ERROR, null, null);
    super.param(DETAILS_KEY, details);
  }
}
