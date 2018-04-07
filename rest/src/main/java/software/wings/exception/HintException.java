package software.wings.exception;

import static software.wings.beans.ErrorCode.EXPLANATION;
import static software.wings.beans.ErrorCode.HINT;
import static software.wings.beans.ResponseMessage.Level.INFO;
import static software.wings.beans.ResponseMessage.aResponseMessage;

public class HintException extends WingsException {
  public static HintException MOVE_TO_THE_PARENT_OBJECT =
      new HintException("Navigate back to the parent object page and continue from there.");
  public static HintException REFRESH_THE_PAGE = new HintException("Refresh the web page to update the data.");

  public HintException(String message) {
    super(aResponseMessage().code(HINT).level(INFO).build());
    super.excludeReportTarget(EXPLANATION, ReportTarget.HARNESS_ENGINEER);
    super.addParam("message", message);
  }
}
