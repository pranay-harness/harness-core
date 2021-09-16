package io.harness.dtos.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReferenceInstanceInfoDTO extends InstanceInfoDTO {
  String podName;
  String namespace;
  String releaseName;

  @Override
  public String prepareInstanceKey() {
    return podName;
  }

  @Override
  public String prepareInstanceSyncHandlerKey() {
    return podName;
  }
}
