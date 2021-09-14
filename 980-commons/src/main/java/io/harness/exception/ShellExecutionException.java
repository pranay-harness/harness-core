/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

public class ShellExecutionException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public ShellExecutionException(String message) {
    super(message, null, ErrorCode.SHELL_EXECUTION_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public ShellExecutionException(String message, Throwable cause) {
    super(message, cause, ErrorCode.SHELL_EXECUTION_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
