/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.REQUEST_PROCESSING_INTERRUPTED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(PL)
public class ProcessingException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public ProcessingException(String message) {
    super(message, null, REQUEST_PROCESSING_INTERRUPTED, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
