package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface defining StateMachine execution callback.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
