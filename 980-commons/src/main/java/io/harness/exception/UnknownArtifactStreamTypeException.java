/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNKNOWN_ARTIFACT_TYPE;

import static java.lang.String.format;

import io.harness.eraro.Level;

public class UnknownArtifactStreamTypeException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnknownArtifactStreamTypeException(String artifactStreamType) {
    super(null, null, UNKNOWN_ARTIFACT_TYPE, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, format("Unknown artifact stream type: %s", artifactStreamType));
  }
}
