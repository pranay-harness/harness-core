/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

  public JsonSchemaValidationException(String message, Throwable cause) {
    super(message, cause, SCHEMA_VALIDATION_FAILED, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
