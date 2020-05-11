package software.wings.api.ondemandrollback;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class OnDemandRollbackInfo {
  private boolean onDemandRollback;
  private String rollbackExecutionId;
}
