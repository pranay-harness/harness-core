package io.harness.delegate.exception;

import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

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
