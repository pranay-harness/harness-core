package io.harness.exception;

import static io.harness.eraro.ErrorCode.JIRA_CLIENT_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(HarnessTeam.CDC)
public class JiraClientException extends WingsException {
  public JiraClientException(String message) {
    super(message, null, JIRA_CLIENT_ERROR, Level.ERROR, null, null);
    super.param("message", message);
  }

  public JiraClientException(String message, Throwable cause) {
    super(message, cause, JIRA_CLIENT_ERROR, Level.ERROR, null, null);
    super.param("message", message);
  }
}
