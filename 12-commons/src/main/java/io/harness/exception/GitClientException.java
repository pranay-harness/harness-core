package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class GitClientException extends WingsException {
  public GitClientException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, ErrorCode.GIT_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
