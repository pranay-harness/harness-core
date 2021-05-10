package io.harness.exception;

import static io.harness.eraro.ErrorCode.ACCESS_DENIED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.CDC)
public class AccessDeniedException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public AccessDeniedException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, ACCESS_DENIED, Level.ERROR, reportTarget, null);
  }

  public AccessDeniedException(String message, ErrorCode errorCode, EnumSet<ReportTarget> reportTarget) {
    super(message, null, errorCode, Level.ERROR, reportTarget, null);
    super.param(MESSAGE_ARG, message);
  }
}
