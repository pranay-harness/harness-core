/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class GitClientException extends WingsException {
  public GitClientException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, ErrorCode.GIT_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }

  public GitClientException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, ErrorCode.GIT_ERROR, Level.ERROR, reportTarget, null);
    super.getParams().put("message", message);
  }
}
