package io.harness.service.instancedashboardservice;

import static java.lang.System.currentTimeMillis;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instance.Instance;
import io.harness.models.BuildsByEnvironment;
import io.harness.models.InstancesByBuild;
import io.harness.models.dashboard.InstanceCountDetails;
import io.harness.models.dashboard.InstanceCountDetailsByService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.repositories.instance.InstanceRepository;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceDashboardServiceImpl implements InstanceDashboardService {
  private InstanceRepository instanceRepository;

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
    List<Instance> instances =
        instanceRepository.getActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, currentTimeMillis());

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
   * API to fetch all active instances for given account+org+project at a given time grouped by environment and build
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param timestampInMs
   * @return List of instances grouped by environment and build
   */
  @Override
  public List<BuildsByEnvironment> getActiveInstancesGroupedByEnvironmentAndBuild(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs) {
    List<Instance> instances =
        instanceRepository.getActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, timestampInMs);

    // used to map a list of instances to build and map it further to environment
    Map<String, Map<String, List<Instance>>> instanceGroupMap = new HashMap<>();
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
      Map<String, Map<String, List<Instance>>> instanceGroupMap) {
    List<BuildsByEnvironment> buildsByEnvironment = new ArrayList<>();
    for (String envId : instanceGroupMap.keySet()) {
      List<InstancesByBuild> instancesByBuilds = new ArrayList<>();
      for (String buildId : instanceGroupMap.get(envId).keySet()) {
        instancesByBuilds.add(new InstancesByBuild(buildId, instanceGroupMap.get(envId).get(buildId)));
      }
      buildsByEnvironment.add(new BuildsByEnvironment(envId, instancesByBuilds));
    }
    return buildsByEnvironment;
  }
}
