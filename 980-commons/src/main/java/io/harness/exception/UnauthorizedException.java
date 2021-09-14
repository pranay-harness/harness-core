/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_TOKEN;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class UnauthorizedException extends WingsException {
  public UnauthorizedException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, INVALID_TOKEN, Level.ERROR, reportTarget, null);
  }

  public UnauthorizedException(String message, ErrorCode errorCode, EnumSet<ReportTarget> reportTarget) {
    super(message, null, errorCode, Level.ERROR, reportTarget, null);
  }
}
