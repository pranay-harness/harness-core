/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
