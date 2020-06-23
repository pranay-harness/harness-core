package software.wings.exception;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

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
