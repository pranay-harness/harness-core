/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION;

import io.harness.eraro.Level;

public class UnsupportedOperationException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public UnsupportedOperationException(String message) {
    super(message, null, UNSUPPORTED_OPERATION_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
