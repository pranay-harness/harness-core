/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.USAGE_RESTRICTION_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class UsageRestrictionException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UsageRestrictionException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, USAGE_RESTRICTION_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }
}
