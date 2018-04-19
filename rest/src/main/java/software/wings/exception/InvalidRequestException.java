package software.wings.exception;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.exception.WingsException.ReportTarget.USER;

public class InvalidRequestException extends WingsException {
  public InvalidRequestException(String message) {
    super(INVALID_REQUEST, USER);
    super.addParam("message", message);
  }

  public InvalidRequestException(String message, ReportTarget... reportTargets) {
    super(INVALID_REQUEST, reportTargets);
    super.addParam("message", message);
  }
}
