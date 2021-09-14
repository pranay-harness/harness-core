/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import static java.lang.String.format;

import io.harness.eraro.Level;

public class UnknownEnumTypeException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnknownEnumTypeException(String typeDisplayName, String value) {
    super(null, null, INVALID_REQUEST, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, format("Unknown %s: %s", typeDisplayName, value));
  }
}
