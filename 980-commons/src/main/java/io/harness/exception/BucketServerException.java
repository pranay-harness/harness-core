/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.BUCKET_SERVER_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(CDP)
public class BucketServerException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public BucketServerException(String message) {
    super(message, null, BUCKET_SERVER_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public BucketServerException(String message, Throwable cause) {
    super(message, cause, BUCKET_SERVER_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public BucketServerException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, BUCKET_SERVER_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }

  public BucketServerException(String message, Throwable cause, EnumSet<WingsException.ReportTarget> reportTargets) {
    super(message, cause, BUCKET_SERVER_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }
}
