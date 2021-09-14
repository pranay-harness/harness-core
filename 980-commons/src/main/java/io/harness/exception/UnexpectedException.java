/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNEXPECTED;

import io.harness.eraro.Level;

public class UnexpectedException extends WingsException {
  public UnexpectedException() {
    super("This should not be happening", null, UNEXPECTED, Level.ERROR, null, null);
  }

  public UnexpectedException(String message) {
    super(message, null, UNEXPECTED, Level.ERROR, null, null);
  }

  public UnexpectedException(String message, Throwable throwable) {
    super(message, throwable, UNEXPECTED, Level.ERROR, null, null);
  }
}
