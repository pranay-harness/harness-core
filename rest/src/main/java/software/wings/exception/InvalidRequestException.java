package software.wings.exception;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;

public class InvalidRequestException extends WingsException {
  public InvalidRequestException(String message, ReportTarget[] reportTargets) {
    super(INVALID_REQUEST, reportTargets);
    super.addParam("message", message);
  }
}
