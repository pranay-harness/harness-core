
package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.eraro.ErrorCode.TIMESCALE_NOT_AVAILABLE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(DX)
public class TimeScaleNotAvailableException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public TimeScaleNotAvailableException(String message) {
    super(message, null, TIMESCALE_NOT_AVAILABLE, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
