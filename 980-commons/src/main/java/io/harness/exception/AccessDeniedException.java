/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.ACCESS_DENIED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.CDC)
public class AccessDeniedException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public AccessDeniedException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, ACCESS_DENIED, Level.ERROR, reportTarget, null);
  }

  public AccessDeniedException(String message, ErrorCode errorCode, EnumSet<ReportTarget> reportTarget) {
    super(message, null, errorCode, Level.ERROR, reportTarget, null);
    super.param(MESSAGE_ARG, message);
  }
}
