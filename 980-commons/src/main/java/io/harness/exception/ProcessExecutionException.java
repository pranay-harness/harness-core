/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.PROCESS_EXECUTION_EXCEPTION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.CDP)
public class ProcessExecutionException extends WingsException {
  public ProcessExecutionException(String message, Throwable cause) {
    super(message, cause, PROCESS_EXECUTION_EXCEPTION, Level.ERROR, USER, EnumSet.of(FailureType.APPLICATION_ERROR));
  }
}
