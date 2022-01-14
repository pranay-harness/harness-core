package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.status.StateStatusUpdateInfo;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface StateStatusUpdate {
  void stateExecutionStatusUpdated(@NotNull StateStatusUpdateInfo stateStatusUpdateInfo);
}
