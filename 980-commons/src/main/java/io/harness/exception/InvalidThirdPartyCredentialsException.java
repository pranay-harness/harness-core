/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
