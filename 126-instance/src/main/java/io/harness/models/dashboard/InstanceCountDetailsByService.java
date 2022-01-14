package io.harness.models.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.EnvironmentType;

import java.util.Map;

@OwnedBy(HarnessTeam.DX)
public class InstanceCountDetailsByService extends InstanceCountDetailsByEnvTypeBase {
  private String serviceId;

  public InstanceCountDetailsByService(Map<EnvironmentType, Integer> envTypeVsInstanceCountMap, String serviceId) {
    super(envTypeVsInstanceCountMap);
    this.serviceId = serviceId;
  }
}
