/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_CREDENTIALS_THIRD_PARTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class InvalidThirdPartyCredentialsException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public InvalidThirdPartyCredentialsException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_CREDENTIALS_THIRD_PARTY, Level.ERROR, reportTargets,
        EnumSet.of(FailureType.AUTHENTICATION));
    super.param(MESSAGE_KEY, message);
  }

  public InvalidThirdPartyCredentialsException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, INVALID_CREDENTIALS_THIRD_PARTY, Level.ERROR, reportTargets,
        EnumSet.of(FailureType.AUTHENTICATION));
    super.param(MESSAGE_KEY, message);
  }
}
