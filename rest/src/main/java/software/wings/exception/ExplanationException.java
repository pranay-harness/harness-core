package software.wings.exception;

import static software.wings.beans.ErrorCode.EXPLANATION;
import static software.wings.beans.ResponseMessage.Level.INFO;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;

public class ExplanationException extends WingsException {
  public ExplanationException(String message, Throwable cause) {
    super(aResponseMessage().code(EXPLANATION).level(INFO).build(), cause);
    super.excludeReportTarget(EXPLANATION, LOG_SYSTEM);
    super.addParam("message", message);
  }
}
