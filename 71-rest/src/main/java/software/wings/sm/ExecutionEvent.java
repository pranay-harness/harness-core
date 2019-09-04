package software.wings.sm;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.EnumSet;

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
