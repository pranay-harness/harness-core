/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.DELEGATE_NOT_AVAILABLE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class DelegateNotAvailableException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public DelegateNotAvailableException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, DELEGATE_NOT_AVAILABLE, Level.ERROR, reportTargets, EnumSet.of(FailureType.APPLICATION_ERROR));
    param(MESSAGE_KEY, message);
  }

  public DelegateNotAvailableException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(
        message, cause, DELEGATE_NOT_AVAILABLE, Level.ERROR, reportTargets, EnumSet.of(FailureType.APPLICATION_ERROR));
    param(MESSAGE_KEY, message);
  }

  public DelegateNotAvailableException(String message, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, DELEGATE_NOT_AVAILABLE, level, reportTargets, EnumSet.of(FailureType.APPLICATION_ERROR));
    param(MESSAGE_KEY, message);
  }
}
