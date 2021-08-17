package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.FailureType;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
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
