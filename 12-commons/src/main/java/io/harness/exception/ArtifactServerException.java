package io.harness.exception;

import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class ArtifactServerException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ArtifactServerException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, ARTIFACT_SERVER_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }
}
