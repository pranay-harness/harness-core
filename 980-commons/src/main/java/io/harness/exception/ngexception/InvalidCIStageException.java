/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception.ngexception;

import static io.harness.eraro.ErrorCode.NG_PIPELINE_CREATE_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InvalidCIStageException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public InvalidCIStageException(String message) {
    super(message, null, NG_PIPELINE_CREATE_EXCEPTION, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public InvalidCIStageException(String message, Throwable cause) {
    super(message, cause, NG_PIPELINE_CREATE_EXCEPTION, Level.ERROR, null, null);
  }
}
