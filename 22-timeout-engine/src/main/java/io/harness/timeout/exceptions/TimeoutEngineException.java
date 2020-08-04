package io.harness.timeout.exceptions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(CDC)
@SuppressWarnings("squid:CallToDeprecatedMethod")
public class TimeoutEngineException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public TimeoutEngineException(String message, Throwable t) {
    super(null, t, ErrorCode.TIMEOUT_ENGINE_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, message);
  }
}
