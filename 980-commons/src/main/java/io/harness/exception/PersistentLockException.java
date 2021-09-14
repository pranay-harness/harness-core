/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(PL)
public class PersistentLockException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public PersistentLockException(String message, ErrorCode code, EnumSet<ReportTarget> reportTarget) {
    super(message, null, code, Level.ERROR, reportTarget, null);
    param(MESSAGE_KEY, message);
  }

  public PersistentLockException(String message, Throwable error, ErrorCode code, EnumSet<ReportTarget> reportTarget) {
    super(message, error, code, Level.ERROR, reportTarget, null);
    param(MESSAGE_KEY, message);
  }
}
