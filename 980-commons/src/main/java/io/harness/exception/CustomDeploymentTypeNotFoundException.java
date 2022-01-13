/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
