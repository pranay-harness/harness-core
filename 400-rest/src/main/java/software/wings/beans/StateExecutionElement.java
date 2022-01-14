package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class StateExecutionElement {
  private String executionContextElementId;
  private ExecutionStatus status;
  private String name;
  private int progress;
  private List<String> runningSteps;
}
