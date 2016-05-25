package software.wings.sm;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface defining StateMachine execution callback.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface StateMachineExecutionCallback {
  void callback(ExecutionContext context, ExecutionStatus status, Exception ex);
}
