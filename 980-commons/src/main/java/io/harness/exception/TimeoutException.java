/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.REQUEST_TIMEOUT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;
@OwnedBy(PL)
public class TimeoutException extends WingsException {
  private static final String NAME_ARG = "name";

  public TimeoutException(String message, String name, EnumSet<ReportTarget> reportTargets) {
    super(message, null, REQUEST_TIMEOUT, Level.ERROR, reportTargets, null);
    super.param(NAME_ARG, name);
  }

  public TimeoutException(String message, String name, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, REQUEST_TIMEOUT, Level.ERROR, reportTargets, null);
    super.param(NAME_ARG, name);
  }
}
