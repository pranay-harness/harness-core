package software.wings.exception;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class ArtifactoryServerException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ArtifactoryServerException(String message, ErrorCode code, EnumSet<ReportTarget> reportTargets) {
    super(message, null, code, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }

  public ArtifactoryServerException(
      String message, ErrorCode code, EnumSet<ReportTarget> reportTargets, Throwable throwable) {
    super(message, throwable, code, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }

  public ArtifactoryServerException(String message, ErrorCode code) {
    super(message, null, code, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, message);
  }
}
