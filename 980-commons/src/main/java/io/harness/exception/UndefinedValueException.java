/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.ILLEGAL_STATE;

import io.harness.eraro.Level;

public class UndefinedValueException extends WingsException {
  public UndefinedValueException(String message) {
    super(message, null, ILLEGAL_STATE, Level.ERROR, null, null);
    param("message", message);
  }
}
