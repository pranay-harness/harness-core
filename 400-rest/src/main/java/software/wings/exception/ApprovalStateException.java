package software.wings.exception;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

@TargetModule(HarnessModule._980_COMMONS)
public class ApprovalStateException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ApprovalStateException(String message, ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, code, level, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
