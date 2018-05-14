package software.wings.exception;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;

public class InvalidRequestException extends WingsException {
  public InvalidRequestException(String message) {
    super(INVALID_REQUEST);
    super.addParam("message", message);
  }

  public InvalidRequestException(String message, Throwable cause) {
    super(INVALID_REQUEST, cause);
    super.addParam("message", message);
  }

  public InvalidRequestException(String message, ReportTarget[] reportTargets) {
    super(INVALID_REQUEST, reportTargets);
    super.addParam("message", message);
  }

  public InvalidRequestException(String message, Throwable cause, ReportTarget[] reportTargets) {
    super(INVALID_REQUEST, reportTargets, cause);
    super.addParam("message", message);
  }
}
