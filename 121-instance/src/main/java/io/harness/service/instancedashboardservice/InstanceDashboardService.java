package io.harness.service.instancedashboardservice;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;
import io.harness.models.dashboard.InstanceCountDetails;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeBase;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface InstanceDashboardService {
  InstanceCountDetails getActiveInstanceCountDetailsByEnvType(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
  List<BuildsByEnvironment> getActiveInstancesByServiceIdGroupedByEnvironmentAndBuild(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);
  List<EnvBuildInstanceCount> getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);
  List<InstancesByBuildId> getActiveInstancesByServiceIdEnvIdAndBuildIds(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String serviceId, String envId, List<String> buildIds, long timestampInMs);
  InstanceCountDetailsByEnvTypeBase getActiveServiceInstanceCountBreakdown(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);
}
