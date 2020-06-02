package io.harness.engine.services;

import static io.harness.eraro.ErrorCode.ENGINE_OUTCOME_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class OutcomeException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public OutcomeException(String message) {
    super(message, null, ENGINE_OUTCOME_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, message);
  }

  public OutcomeException(String message, Throwable cause) {
    super(message, cause, ENGINE_OUTCOME_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, message);
  }
}
