package io.harness.delegate.beans.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class NativeHelmInstanceSyncPerpetualTaskResponse implements InstanceSyncPerpetualTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private List<ServerInstanceInfo> serverInstanceDetails;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
