/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.POD_NOT_FOUND_ERROR;

import io.harness.eraro.Level;

public class PodNotFoundException extends WingsException {
  private static final String REASON_ARG = "reason";

  public PodNotFoundException(String reason) {
    this(reason, null);
  }

  public PodNotFoundException(String reason, Throwable cause) {
    super(null, cause, POD_NOT_FOUND_ERROR, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
  }
}
