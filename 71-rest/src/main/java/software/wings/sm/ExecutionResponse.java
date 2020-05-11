package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;

import java.util.EnumSet;
import java.util.List;

/**
 * Describes response of an execution.
 */
@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
public class ExecutionResponse {
  private boolean async;
  @Singular private List<String> correlationIds;
  @Default private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
  private EnumSet<FailureType> failureTypes;
  private String errorMessage;
  private StateExecutionData stateExecutionData;
  @Singular private List<ContextElement> notifyElements;
  @Singular private List<ContextElement> contextElements;
  private String delegateTaskId;
  @Singular private List<StateExecutionInstance> stateExecutionInstances;
}
