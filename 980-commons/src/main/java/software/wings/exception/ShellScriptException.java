package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class ShellScriptException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ShellScriptException(String message, ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, code, level, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
