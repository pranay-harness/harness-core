/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public abstract class NoDelegatesException extends WingsException {
  public NoDelegatesException(String message, ErrorCode errorCode) {
    super(message, null, errorCode, Level.ERROR, USER, null);
  }
}
