package software.wings.exception;

import static io.harness.eraro.Level.INFO;
import static software.wings.beans.ErrorCode.EXPLANATION;
import static software.wings.beans.ErrorCode.HINT;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;

public class HintException extends WingsException {
  public static final HintException MOVE_TO_THE_PARENT_OBJECT =
      new HintException("Navigate back to the parent object page and continue from there.");
  public static final HintException REFRESH_THE_PAGE = new HintException("Refresh the web page to update the data.");

  public HintException(String message) {
    super(aResponseMessage().code(HINT).level(INFO).build());
    super.excludeReportTarget(EXPLANATION, LOG_SYSTEM);
    super.addParam("message", message);
  }
}
