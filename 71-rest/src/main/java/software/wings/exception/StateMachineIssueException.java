package software.wings.exception;

import static io.harness.eraro.ErrorCode.STATE_MACHINE_ISSUE;

import io.harness.exception.WingsException;

public class StateMachineIssueException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public StateMachineIssueException(String details) {
    super(STATE_MACHINE_ISSUE);
    super.addParam(DETAILS_KEY, details);
  }
}
