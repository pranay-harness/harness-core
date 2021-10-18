package io.harness.exception.ngexception;

import static io.harness.eraro.ErrorCode.TEMPLATE_EXCEPTION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.CDC)
public class NGTemplateException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public NGTemplateException(String message) {
    super(message, null, TEMPLATE_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public NGTemplateException(String message, Throwable cause) {
    super(message, cause, TEMPLATE_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public NGTemplateException(String message, Throwable cause, EnumSet<ReportTarget> reportTarget) {
    super(message, cause, TEMPLATE_EXCEPTION, Level.ERROR, reportTarget, null);
    super.param(MESSAGE_ARG, message);
  }
}
