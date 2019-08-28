package software.wings.sm;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionEvent {
  private ExecutionContextImpl context;
  private State state;

  public ExecutionStatus getExecutionStatus() {
    if (context == null) {
      return null;
    }

    return context.getStateExecutionInstance().getStatus();
  }
}
