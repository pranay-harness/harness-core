package io.harness.engine.executions.node;

import static io.harness.eraro.ErrorCode.ENGINE_ENTITY_UPDATE_EXCEPTION;
import static io.harness.eraro.ErrorCode.ENGINE_OUTCOME_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class NodeExecutionUpdateFailedException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public NodeExecutionUpdateFailedException(String message) {
    super(message, null, ENGINE_ENTITY_UPDATE_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, message);
  }

  public NodeExecutionUpdateFailedException(String message, Throwable cause) {
    super(message, cause, ENGINE_OUTCOME_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, message);
  }
}
