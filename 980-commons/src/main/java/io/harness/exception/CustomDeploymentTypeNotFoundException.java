/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.TEMPLATE_NOT_FOUND;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class CustomDeploymentTypeNotFoundException extends WingsException {
  private static final String MESSAGE_KEY = "message";
  // This is a new method, and does not override any deprecated method.
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public CustomDeploymentTypeNotFoundException(
      String message, Throwable throwable, EnumSet<ReportTarget> reportTargets) {
    super(message, throwable, TEMPLATE_NOT_FOUND, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public CustomDeploymentTypeNotFoundException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, TEMPLATE_NOT_FOUND, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
