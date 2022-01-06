/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class InvalidArtifactServerException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public InvalidArtifactServerException(String message) {
    super(null, null, INVALID_ARTIFACT_SERVER, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public InvalidArtifactServerException(String message, Throwable cause) {
    super(message, cause, INVALID_ARTIFACT_SERVER, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public InvalidArtifactServerException(String message, EnumSet<ReportTarget> reportTargets) {
    super(null, null, INVALID_ARTIFACT_SERVER, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public InvalidArtifactServerException(String message, Level level, EnumSet<ReportTarget> reportTargets) {
    super(null, null, INVALID_ARTIFACT_SERVER, level, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
