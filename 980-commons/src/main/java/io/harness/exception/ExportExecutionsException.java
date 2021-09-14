/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ExportExecutionsException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ExportExecutionsException(String message) {
    super(message, null, GENERAL_ERROR, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public ExportExecutionsException(String message, Throwable throwable) {
    super(message, throwable, GENERAL_ERROR, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }
}
