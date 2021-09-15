/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient.exception;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;
import java.util.Map;

public class SecretManagementClientException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public SecretManagementClientException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message, null);
  }

  public SecretManagementClientException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    super(null, null, errorCode, Level.ERROR, reportTargets, null);
  }

  public SecretManagementClientException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    super(null, null, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public SecretManagementClientException(ErrorCode errorCode, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
  }

  public SecretManagementClientException(
      ErrorCode errorCode, String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public SecretManagementClientException(
      ErrorCode errorCode, Throwable cause, EnumSet<ReportTarget> reportTargets, Map<String, String> params) {
    super(null, cause, errorCode, Level.ERROR, reportTargets, null);
    if (EmptyPredicate.isNotEmpty(params)) {
      params.forEach(this::param);
    }
  }
}
