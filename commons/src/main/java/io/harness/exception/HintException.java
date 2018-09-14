package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPLANATION;
import static io.harness.eraro.ErrorCode.HINT;
import static io.harness.eraro.Level.INFO;
import static io.harness.exception.WingsException.ReportTarget.LOG_SYSTEM;

public class HintException extends WingsException {
  public static final HintException MOVE_TO_THE_PARENT_OBJECT =
      new HintException("Navigate back to the parent object page and continue from there.");
  public static final HintException REFRESH_THE_PAGE = new HintException("Refresh the web page to update the data.");

  public HintException(String message) {
    super(null, null, HINT, INFO, null);
    super.excludeReportTarget(EXPLANATION, LOG_SYSTEM);
    super.addParam("message", message);
  }
}
