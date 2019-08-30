package software.wings.sm;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Describes response of an execution.
 */
@Value
@Builder(toBuilder = true)
public class ExecutionResponse {
  private boolean async;
  @Singular private List<String> correlationIds;
  @Default private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
  private String errorMessage;
  private StateExecutionData stateExecutionData;
  @Singular private List<ContextElement> notifyElements;
  @Singular private List<ContextElement> contextElements;
  private String delegateTaskId;
  @Singular private List<StateExecutionInstance> stateExecutionInstances;
}
