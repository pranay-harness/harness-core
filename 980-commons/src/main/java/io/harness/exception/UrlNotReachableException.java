/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

public class UrlNotReachableException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public UrlNotReachableException(String message) {
    super(message, null, ErrorCode.URL_NOT_REACHABLE, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
