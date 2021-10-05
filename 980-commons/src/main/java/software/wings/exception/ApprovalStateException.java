package software.wings.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

@OwnedBy(PL)
public class ApprovalStateException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ApprovalStateException(String message, ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, code, level, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
