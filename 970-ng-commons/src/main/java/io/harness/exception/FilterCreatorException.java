/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eraro.ErrorCode.FILTER_CREATION_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(PIPELINE)
public class FilterCreatorException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public FilterCreatorException(String message) {
    super(message, null, FILTER_CREATION_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public FilterCreatorException(String message, Throwable cause) {
    super(message, cause, FILTER_CREATION_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
