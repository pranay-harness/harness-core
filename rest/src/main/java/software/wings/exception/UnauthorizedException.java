package software.wings.exception;

import static software.wings.beans.ErrorCode.INVALID_TOKEN;

public class UnauthorizedException extends WingsException {
  public UnauthorizedException(String message, ReportTarget[] reportTarget) {
    super(INVALID_TOKEN, message, reportTarget);
  }
}
