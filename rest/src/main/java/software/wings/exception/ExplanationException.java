package software.wings.exception;

import static io.harness.eraro.ErrorCode.EXPLANATION;
import static io.harness.eraro.Level.INFO;
import static io.harness.exception.WingsException.ReportTarget.LOG_SYSTEM;

import io.harness.exception.WingsException;

public class ExplanationException extends WingsException {
  public ExplanationException(String message, Throwable cause) {
    super(null, cause, EXPLANATION, INFO, USER_SRE);
    super.excludeReportTarget(EXPLANATION, LOG_SYSTEM);
    super.addParam("message", message);
  }
}
