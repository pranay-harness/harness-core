/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.Level.ERROR;

import io.harness.eraro.ErrorCode;

/**
 * Indicates an exception due to data format issues.
 */
public class DataFormatException extends WingsException {
  // This is a new method, and does not override any deprecated method.
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public DataFormatException(String message, Throwable cause) {
    super(message, cause, ErrorCode.UNKNOWN_ERROR, ERROR, SRE, null);
  }
}
