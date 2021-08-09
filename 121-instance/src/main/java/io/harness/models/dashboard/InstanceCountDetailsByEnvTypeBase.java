package io.harness.models.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.EnvironmentType;

import java.util.Map;
import lombok.Builder;

@Builder
@OwnedBy(HarnessTeam.DX)
public class InstanceCountDetailsByEnvTypeBase {
  private Map<EnvironmentType, Integer> envTypeVsInstanceCountMap;

  public Integer getNonProdInstances() {
    return envTypeVsInstanceCountMap.getOrDefault(EnvironmentType.PreProduction, 0);
  }

  public Integer getProdInstances() {
    return envTypeVsInstanceCountMap.getOrDefault(EnvironmentType.Production, 0);
  }

  public Integer getTotalInstances() {
    return getNonProdInstances() + getProdInstances();
  }
}
