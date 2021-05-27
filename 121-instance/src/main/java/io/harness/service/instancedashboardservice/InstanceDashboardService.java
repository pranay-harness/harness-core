package io.harness.service.instancedashboardservice;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.dashboard.InstanceCountDetails;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface InstanceDashboardService {
  InstanceCountDetails getActiveInstanceCountDetailsByEnvType(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
  List<BuildsByEnvironment> getActiveInstancesGroupedByEnvironmentAndBuild(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);
}
