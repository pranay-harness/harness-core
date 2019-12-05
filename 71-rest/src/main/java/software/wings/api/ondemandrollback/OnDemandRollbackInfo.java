package software.wings.api.ondemandrollback;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnDemandRollbackInfo {
  private boolean onDemandRollback;
  private String rollbackExecutionId;
}
