package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;

/**
 * Interface defining StateMachine execution callback.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface StateMachineExecutionCallback {
  /**
   * Callback.
   *
   * @param context the context
   * @param status  the status
   * @param ex      the ex
   */
  void callback(ExecutionContext context, ExecutionStatus status, Exception ex);
}
