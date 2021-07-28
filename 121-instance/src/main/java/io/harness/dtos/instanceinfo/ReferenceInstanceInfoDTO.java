package io.harness.dtos.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
public class ReferenceInstanceInfoDTO extends InstanceInfoDTO {
  String podName;

  @Override
  public String prepareInstanceKey() {
    return podName;
  }

  @Override
  public String prepareInstanceSyncHandlerKey() {
    return podName;
  }
}
