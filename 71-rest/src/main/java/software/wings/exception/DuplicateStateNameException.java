package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class DuplicateStateNameException extends WingsException {
  private static final String DUPLICATE_STATE_KEY = "dupStateNames";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public DuplicateStateNameException(String details) {
    super(null, null, ErrorCode.DUPLICATE_STATE_NAMES, Level.ERROR, null, null);
    super.param(DUPLICATE_STATE_KEY, details);
  }
}