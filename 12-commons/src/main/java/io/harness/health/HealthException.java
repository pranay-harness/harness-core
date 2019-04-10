package io.harness.health;

import static io.harness.eraro.ErrorCode.HEALTH_ERROR;

import io.harness.exception.WingsException;

public class HealthException extends WingsException {
  private static final String REASON_KEY = "reason";

  public HealthException(String reason) {
    super(HEALTH_ERROR);
    addParam(REASON_KEY, reason);
  }

  public HealthException(String reason, Throwable cause) {
    super(HEALTH_ERROR, cause);
    addParam(REASON_KEY, reason);
  }
}
