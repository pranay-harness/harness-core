/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(PL)
public class SignupException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public SignupException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, USER, null);
    super.param(MESSAGE_ARG, message);
  }

  public SignupException(String message, Throwable cause, ErrorCode code, Level level,
      EnumSet<ReportTarget> reportTargets, EnumSet<FailureType> failureTypes) {
    super(message, cause, code, level, reportTargets, failureTypes);
    param(MESSAGE_ARG, message);
  }
}
