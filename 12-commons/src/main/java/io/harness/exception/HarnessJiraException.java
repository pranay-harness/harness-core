package io.harness.exception;

import static io.harness.eraro.ErrorCode.JIRA_ERROR;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class HarnessJiraException extends WingsException {
  public HarnessJiraException(String message, Throwable cause, ErrorCode code, Level level,
      EnumSet<ReportTarget> reportTargets, EnumSet<FailureType> failureTypes) {
    super(message, cause, JIRA_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }
}
