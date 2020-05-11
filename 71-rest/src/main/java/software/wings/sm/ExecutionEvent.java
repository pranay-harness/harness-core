package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.EnumSet;

@OwnedBy(CDC)
@Value
@Builder
public class ExecutionEvent {
  private EnumSet<FailureType> failureTypes;
  private ExecutionContextImpl context;
  private State state;

  public ExecutionStatus getExecutionStatus() {
    if (context == null) {
      return null;
    }

    return context.getStateExecutionInstance().getStatus();
  }
}
