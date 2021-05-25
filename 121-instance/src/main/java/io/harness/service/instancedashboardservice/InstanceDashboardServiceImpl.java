package io.harness.service.instancedashboardservice;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instance.Instance;
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
        instanceRepository.getActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier);

    Map<String, Map<EnvironmentType, Integer>> serviceVsInstanceCountMap = new HashMap<>();
    instances.forEach(instance -> {
      if (!serviceVsInstanceCountMap.containsKey(instance.getServiceId())) {
        serviceVsInstanceCountMap.put(instance.getServiceId(), new HashMap<>());
      }
      incrementValueForGivenEnvType(serviceVsInstanceCountMap.get(instance.getServiceId()), instance.getEnvType(), 1);
    });

    return prepareInstanceCountDetailsResponse(serviceVsInstanceCountMap);
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
}
