/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
