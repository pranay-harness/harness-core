/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_YAML_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(HarnessTeam.PIPELINE)
public class InvalidYamlException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public InvalidYamlException(String message) {
    super(message, null, INVALID_YAML_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public InvalidYamlException(String message, Throwable cause) {
    super(message, cause, INVALID_YAML_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
