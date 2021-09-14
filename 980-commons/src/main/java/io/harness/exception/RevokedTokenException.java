/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.REVOKED_TOKEN;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class RevokedTokenException extends WingsException {
  public RevokedTokenException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, REVOKED_TOKEN, Level.ERROR, reportTargets, null);
  }
}
