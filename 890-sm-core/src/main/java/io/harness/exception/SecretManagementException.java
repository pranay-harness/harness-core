/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;
import java.util.Map;

/**
 * @author marklu on 9/19/19
 */
@OwnedBy(PL)
public class SecretManagementException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public SecretManagementException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message, null);
  }

  public SecretManagementException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    super(null, null, errorCode, Level.ERROR, reportTargets, null);
  }

  public SecretManagementException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public SecretManagementException(ErrorCode errorCode, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
  }

  public SecretManagementException(
      ErrorCode errorCode, String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public SecretManagementException(
      ErrorCode errorCode, Throwable cause, EnumSet<ReportTarget> reportTargets, Map<String, String> params) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
    if (EmptyPredicate.isNotEmpty(params)) {
      params.forEach(this::param);
    }
  }
}
