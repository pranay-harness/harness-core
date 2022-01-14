package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.JIRA_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(CDC)
public class HarnessJiraException extends WingsException {
  public HarnessJiraException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, JIRA_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }

  public HarnessJiraException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, JIRA_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }

  public HarnessJiraException(String message) {
    super(message, null, JIRA_ERROR, Level.ERROR, null, null);
    super.param("message", message);
  }
}
