/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class ApprovalStateException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ApprovalStateException(String message, ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, code, level, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
