package io.harness.exception;

import static io.harness.eraro.ErrorCode.FEATURE_UNAVAILABLE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(HarnessTeam.PL)
public class UnavailableFeatureException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnavailableFeatureException(String message) {
    super(null, null, FEATURE_UNAVAILABLE, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, message);
  }
}
