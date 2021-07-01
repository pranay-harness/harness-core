package io.harness.service.instancedashboardservice;

import static java.lang.System.currentTimeMillis;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InstanceDTO;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.models.dashboard.InstanceCountDetails;
import io.harness.models.dashboard.InstanceCountDetailsByService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.service.instanceService.InstanceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

@Singleton
@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceDashboardServiceImpl implements InstanceDashboardService {
  private InstanceService instanceService;
  /**
   * API to fetch active instance count overview for given account+org+project group by env type
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @return total overall instance count group by env type combined with same details per service level
   */
  @Override
  public InstanceCountDetails getActiveInstanceCountDetailsByEnvType(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<InstanceDTO> instances =
        instanceService.getActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, currentTimeMillis());

    Map<String, Map<EnvironmentType, Integer>> serviceVsInstanceCountMap = new HashMap<>();
    instances.forEach(instance -> {
      if (!serviceVsInstanceCountMap.containsKey(instance.getServiceId())) {
        serviceVsInstanceCountMap.put(instance.getServiceId(), new HashMap<>());
      }
      incrementValueForGivenEnvType(serviceVsInstanceCountMap.get(instance.getServiceId()), instance.getEnvType(), 1);
    });

    return prepareInstanceCountDetailsResponse(serviceVsInstanceCountMap);
  }

  /**
   * API to fetch all active instances for given account+org+project+service at a given time grouped by environment and
   * build
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param timestampInMs
   * @return List of instances grouped by environment and build
   */
  @Override
  public List<BuildsByEnvironment> getActiveInstancesByServiceIdGroupedByEnvironmentAndBuild(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs) {
    List<InstanceDTO> instances = instanceService.getActiveInstancesByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs);

    // used to map a list of instances to build and map it further to environment
    Map<String, Map<String, List<InstanceDTO>>> instanceGroupMap = new HashMap<>();
    instances.forEach(instance -> {
      String envId = instance.getEnvId();
      String buildId = instance.getPrimaryArtifact().getTag();
      if (!instanceGroupMap.containsKey(envId)) {
        instanceGroupMap.put(envId, new HashMap<>());
      }
      if (!instanceGroupMap.get(envId).containsKey(buildId)) {
        instanceGroupMap.get(envId).put(buildId, new ArrayList());
      }
      instanceGroupMap.get(envId).get(buildId).add(instance);
    });

    return prepareInstanceGroupedByEnvironmentAndBuildData(instanceGroupMap);
  }

  /**
   * API to fetch all unique combinations of environment and build with instance count
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param serviceId
   * @param timestampInMs
   * @return List of unique environment and build ids with instance count
   */
  @Override
  public List<EnvBuildInstanceCount> getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs) {
    AggregationResults<EnvBuildInstanceCount> envBuildInstanceCountAggregationResults =
        instanceService.getEnvBuildInstanceCountByServiceId(
            accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs);
    List<EnvBuildInstanceCount> envBuildInstanceCounts = new ArrayList<>();

    envBuildInstanceCountAggregationResults.getMappedResults().forEach(envBuildInstanceCount -> {
      final String envId = envBuildInstanceCount.getEnvId();
      final String envName = envBuildInstanceCount.getEnvName();
      final String buildId = envBuildInstanceCount.getTag();
      final Integer count = envBuildInstanceCount.getCount();
      envBuildInstanceCounts.add(new EnvBuildInstanceCount(envId, envName, buildId, count));
    });

    return envBuildInstanceCounts;
  }

  /**
   * API to fetch all active instances for given account+org+project+service+env and list of buildIds at a given time
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param serviceId
   * @param envId
   * @param buildIds
   * @param timestampInMs
   * @return List of buildId and instances
   */
  @Override
  public List<InstancesByBuildId> getActiveInstancesByServiceIdEnvIdAndBuildIds(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String serviceId, String envId, List<String> buildIds,
      long timestampInMs) {
    AggregationResults<InstancesByBuildId> buildIdAndInstancesAggregationResults =
        instanceService.getActiveInstancesByServiceIdEnvIdAndBuildIds(accountIdentifier, orgIdentifier,
            projectIdentifier, serviceId, envId, buildIds, timestampInMs, InstanceSyncConstants.INSTANCE_LIMIT);
    List<InstancesByBuildId> buildIdAndInstancesList = new ArrayList<>();

    buildIdAndInstancesAggregationResults.getMappedResults().forEach(buildIdAndInstances -> {
      String buildId = buildIdAndInstances.getBuildId();
      List<InstanceDTO> instances = buildIdAndInstances.getInstances();
      buildIdAndInstancesList.add(new InstancesByBuildId(buildId, instances));
    });

    return buildIdAndInstancesList;
  }

  // ----------------------------- PRIVATE METHODS -----------------------------

  private InstanceCountDetails prepareInstanceCountDetailsResponse(
      Map<String, Map<EnvironmentType, Integer>> serviceVsInstanceCountMap) {
    Map<EnvironmentType, Integer> envTypeVsIntegerCountMap = new HashMap<>();
    List<InstanceCountDetailsByService> instanceCountDetailsByServiceList = new ArrayList<>();

    serviceVsInstanceCountMap.keySet().forEach(serviceId -> {
      instanceCountDetailsByServiceList.add(
          new InstanceCountDetailsByService(serviceVsInstanceCountMap.get(serviceId), serviceId));
      incrementValueForGivenEnvType(envTypeVsIntegerCountMap, EnvironmentType.PreProduction,
          serviceVsInstanceCountMap.get(serviceId).get(EnvironmentType.PreProduction));
      incrementValueForGivenEnvType(envTypeVsIntegerCountMap, EnvironmentType.Production,
          serviceVsInstanceCountMap.get(serviceId).get(EnvironmentType.Production));
    });

    return new InstanceCountDetails(envTypeVsIntegerCountMap, instanceCountDetailsByServiceList);
  }

  private void incrementValueForGivenEnvType(
      Map<EnvironmentType, Integer> envTypeVsIntegerCountMap, EnvironmentType environmentType, int value) {
    envTypeVsIntegerCountMap.put(environmentType, value + envTypeVsIntegerCountMap.getOrDefault(environmentType, 0));
  }

  private List<BuildsByEnvironment> prepareInstanceGroupedByEnvironmentAndBuildData(
      Map<String, Map<String, List<InstanceDTO>>> instanceGroupMap) {
    List<BuildsByEnvironment> buildsByEnvironment = new ArrayList<>();
    for (String envId : instanceGroupMap.keySet()) {
      List<InstancesByBuildId> instancesByBuilds = new ArrayList<>();
      for (String buildId : instanceGroupMap.get(envId).keySet()) {
        instancesByBuilds.add(new InstancesByBuildId(buildId, instanceGroupMap.get(envId).get(buildId)));
      }
      buildsByEnvironment.add(new BuildsByEnvironment(envId, instancesByBuilds));
    }
    return buildsByEnvironment;
  }
}
