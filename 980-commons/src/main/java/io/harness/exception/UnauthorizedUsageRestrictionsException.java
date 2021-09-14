/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class UnauthorizedUsageRestrictionsException extends WingsException {
  public UnauthorizedUsageRestrictionsException(EnumSet<ReportTarget> reportTarget) {
    super(null, null, USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, Level.ERROR, reportTarget, null);
  }
}
