/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InvalidRollbackException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public InvalidRollbackException(String details, ErrorCode errorCode) {
    super(null, null, errorCode, Level.ERROR, null, null);
    super.param(DETAILS_KEY, details);
  }
}
