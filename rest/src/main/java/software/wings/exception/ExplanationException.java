package software.wings.exception;

import static software.wings.beans.ErrorCode.EXPLANATION;
import static software.wings.beans.ResponseMessage.Level.INFO;
import static software.wings.beans.ResponseMessage.aResponseMessage;

public class ExplanationException extends WingsException {
  public ExplanationException(String message, Throwable cause) {
    super(aResponseMessage().code(EXPLANATION).level(INFO).build(), cause);
    super.excludeReportTarget(EXPLANATION, ReportTarget.HARNESS_ENGINEER);
    super.addParam("message", message);
  }
}
