/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class IndexManagerInspectException extends WingsException {
  public IndexManagerInspectException() {
    super(null, null, GENERAL_ERROR, Level.ERROR, null, null);
  }

  public IndexManagerInspectException(String message) {
    super(message, null, GENERAL_ERROR, Level.ERROR, null, null);
  }
}
