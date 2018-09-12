package software.wings.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.exception.WingsException;

import java.util.EnumSet;

public class InvalidRequestException extends WingsException {
  public InvalidRequestException(String message) {
    super(INVALID_REQUEST);
    super.addParam("message", message);
  }

  public InvalidRequestException(String message, Throwable cause) {
    super(INVALID_REQUEST, cause);
    super.addParam("message", message);
  }

  public InvalidRequestException(String message, EnumSet<ReportTarget> reportTargets) {
    super(INVALID_REQUEST, reportTargets);
    super.addParam("message", message);
  }

  public InvalidRequestException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(INVALID_REQUEST, reportTargets, cause);
    super.addParam("message", message);
  }
}
