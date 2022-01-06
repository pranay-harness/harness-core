package software.wings.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(HarnessTeam.CDC)
// This is special exception to handle ticket: https://harness.atlassian.net/browse/CDC-15969.
// Do not use this exception anywhere else.
public class StateExecutionInstanceUpdateException extends WingsException {
  public StateExecutionInstanceUpdateException(String message) {
    super(message, null, null, Level.ERROR, null, null);
  }
}
