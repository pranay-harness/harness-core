package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eraro.ErrorCode.SCHEMA_VALIDATION_FAILED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(PIPELINE)
public class JsonSchemaValidationException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public JsonSchemaValidationException(String message) {
    super(message, null, SCHEMA_VALIDATION_FAILED, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
