package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class ImageNotFoundException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  // This method does not create the intended message, needs to be fixed @George
  public ImageNotFoundException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public ImageNotFoundException(String message, Throwable cause) {
    super(message, cause, INVALID_REQUEST, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public ImageNotFoundException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_REQUEST, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }

  public ImageNotFoundException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, INVALID_REQUEST, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }

  public ImageNotFoundException(String message, ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    super(message, null, errorCode, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }

  public ImageNotFoundException(
      String message, Throwable cause, ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, errorCode, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }
}
