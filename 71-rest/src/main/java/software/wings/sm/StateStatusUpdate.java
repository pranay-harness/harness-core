package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.sm.status.StateStatusUpdateInfo;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface StateStatusUpdate {
  void stateExecutionStatusUpdated(@NotNull StateStatusUpdateInfo stateStatusUpdateInfo);
}
