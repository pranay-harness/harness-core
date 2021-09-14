/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.NON_EXISTING_PIPELINE;

import io.harness.eraro.Level;

public class PipelineDoesNotExistException extends WingsException {
  private static final String MESSAGE_ARG = "message";
  private static final String MESSAGE = "Pipeline with Id: %s does not exist.";

  public PipelineDoesNotExistException(String pipelineId) {
    super(String.format(MESSAGE, pipelineId), null, NON_EXISTING_PIPELINE, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, MESSAGE);
  }
}
