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
