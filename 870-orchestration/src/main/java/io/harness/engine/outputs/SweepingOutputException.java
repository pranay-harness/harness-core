package io.harness.engine.outputs;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eraro.ErrorCode.ENGINE_SWEEPING_OUTPUT_EXCEPTION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(PIPELINE)
@SuppressWarnings("squid:CallToDeprecatedMethod")
public class SweepingOutputException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public SweepingOutputException(String message) {
    super(message, null, ENGINE_SWEEPING_OUTPUT_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, message);
  }

  public SweepingOutputException(String message, Throwable cause) {
    super(message, cause, ENGINE_SWEEPING_OUTPUT_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, message);
  }
}
