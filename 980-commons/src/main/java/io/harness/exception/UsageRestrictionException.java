/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
