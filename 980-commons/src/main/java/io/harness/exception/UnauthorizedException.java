/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
