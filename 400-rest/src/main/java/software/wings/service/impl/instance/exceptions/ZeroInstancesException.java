package software.wings.service.impl.instance.exceptions;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class ZeroInstancesException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ZeroInstancesException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, ErrorCode.DEFAULT_ERROR_CODE, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
