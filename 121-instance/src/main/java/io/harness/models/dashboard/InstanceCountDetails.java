package io.harness.models.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.EnvironmentType;

import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
@OwnedBy(HarnessTeam.DX)
public class InstanceCountDetails extends InstanceCountDetailsBase {
  private List<InstanceCountDetailsByService> instanceCountDetailsByServiceList;

  public InstanceCountDetails(Map<EnvironmentType, Integer> envTypeVsInstanceCountMap,
      List<InstanceCountDetailsByService> instanceCountDetailsByServiceList) {
    super(envTypeVsInstanceCountMap);
    this.instanceCountDetailsByServiceList = instanceCountDetailsByServiceList;
  }
}
