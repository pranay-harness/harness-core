/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.SECRET_NOT_FOUND;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class SecretNotFoundException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public SecretNotFoundException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, SECRET_NOT_FOUND, Level.ERROR, reportTargets, EnumSet.of(FailureType.APPLICATION_ERROR));
    param(MESSAGE_KEY, message);
  }
}
