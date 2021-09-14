/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

public class TaskFailureException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public TaskFailureException(String message) {
    this(message, null);
  }

  public TaskFailureException(String message, Throwable cause) {
    super(null, cause, ErrorCode.TASK_FAILURE_ERROR, Level.ERROR, USER_SRE, null);
    super.param(MESSAGE_ARG, message);
  }
}
