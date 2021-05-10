package io.harness.dto.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@OwnedBy(HarnessTeam.DX)
public abstract class ContainerInfo extends InstanceInfo {
  private String clusterName;

  public ContainerInfo(String clusterName) {
    this.clusterName = clusterName;
  }
}
