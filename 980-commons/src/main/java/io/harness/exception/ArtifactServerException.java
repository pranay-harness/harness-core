/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class ArtifactServerException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public ArtifactServerException(String message) {
    super(message, null, ARTIFACT_SERVER_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public ArtifactServerException(String message, Throwable cause) {
    super(message, cause, ARTIFACT_SERVER_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public ArtifactServerException(String message, EnumSet<WingsException.ReportTarget> reportTargets) {
    super(message, null, ARTIFACT_SERVER_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }

  public ArtifactServerException(String message, Throwable cause, EnumSet<WingsException.ReportTarget> reportTargets) {
    super(message, cause, ARTIFACT_SERVER_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }
}
