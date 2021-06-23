package io.harness.exception;

import static io.harness.eraro.ErrorCode.JIRA_STEP_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import lombok.Getter;

@OwnedBy(HarnessTeam.CDC)
public class JiraStepException extends WingsException {
  @Getter private final boolean fatal;

  public JiraStepException(String message, boolean fatal, Throwable cause) {
    super(message, cause, JIRA_STEP_ERROR, Level.ERROR, null, null);
    super.param("message", message);
    this.fatal = fatal;
  }

  public JiraStepException(String message, boolean fatal) {
    this(message, fatal, null);
  }

  public JiraStepException(String message) {
    this(message, false, null);
  }
}
