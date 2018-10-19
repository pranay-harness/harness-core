package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPLANATION;
import static io.harness.eraro.Level.INFO;

import java.util.EnumSet;

public class ExplanationException extends WingsException {
  public ExplanationException(String message, Throwable cause) {
    super(null, cause, EXPLANATION, INFO, USER_SRE);
    super.excludeReportTarget(EXPLANATION, EnumSet.of(ReportTarget.LOG_SYSTEM));
    super.addParam("message", message);
  }
}
