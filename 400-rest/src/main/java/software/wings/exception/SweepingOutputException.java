/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.exception;

import static io.harness.eraro.ErrorCode.STATE_MACHINE_ISSUE;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class SweepingOutputException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public SweepingOutputException(String details) {
    super(null, null, STATE_MACHINE_ISSUE, Level.ERROR, null, null);
    param(DETAILS_KEY, details);
  }
}
