package software.wings.service;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.AzureWebAppInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AzureWebAppInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AzureWebAppDeploymentKey;
import software.wings.service.intfc.instance.InstanceService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureWebAppInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  private static final String APP_NAME = "appName";
  private static final String SLOT_NAME = "slotName";

  @Inject private InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Set<AzureWebAppInstanceInfo> webAppInstancesInfo =
        getWebAppInstancesInfo(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    return createPerpetualTasks(webAppInstancesInfo, infrastructureMapping);
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    Set<String> existingWebAppNames = existingPerpetualTasks.stream()
                                          .map(task -> task.getClientContext().getClientParams())
                                          .map(params -> params.get(APP_NAME))
                                          .collect(Collectors.toSet());

    List<AzureWebAppDeploymentKey> newDeploymentWebAppKeys =
        deploymentSummaries.stream().map(DeploymentSummary::getAzureWebAppDeploymentKey).collect(Collectors.toList());

    Set<String> newDeploymentWebAppNames =
        newDeploymentWebAppKeys.stream().map(AzureWebAppDeploymentKey::getAppName).collect(Collectors.toSet());

    Sets.SetView<String> newDeployedWebAppNames = Sets.difference(newDeploymentWebAppNames, existingWebAppNames);

    Set<AzureWebAppInstanceInfo> webAppInstancesInfo =
        newDeploymentWebAppKeys.stream()
            .filter(newDeployedWebAppKey -> newDeployedWebAppNames.contains(newDeployedWebAppKey.getAppName()))
            .map(newDeployedWebAppKey
                -> AzureWebAppInstanceInfo.builder()
                       .appName(newDeployedWebAppKey.getAppName())
                       .slotName(newDeployedWebAppKey.getSlotName())
                       .build())
            .collect(Collectors.toSet());

    return createPerpetualTasks(webAppInstancesInfo, infrastructureMapping);
  }

  private List<String> createPerpetualTasks(
      Set<AzureWebAppInstanceInfo> webAppInstancesInfo, InfrastructureMapping infrastructureMapping) {
    String appId = infrastructureMapping.getAppId();
    String infraMappingId = infrastructureMapping.getUuid();
    String accountId = infrastructureMapping.getAccountId();

    return webAppInstancesInfo.stream()
        .map(azureWebAppInstanceInfo
            -> AzureWebAppInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(appId)
                   .infraMappingId(infraMappingId)
                   .appName(azureWebAppInstanceInfo.getAppName())
                   .slotName(azureWebAppInstanceInfo.getSlotName())
                   .build())
        .map(params -> create(accountId, params))
        .collect(Collectors.toList());
  }

  private String create(String accountId, AzureWebAppInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> paramMap = ImmutableMap.of(HARNESS_APPLICATION_ID, clientParams.getAppId(),
        INFRASTRUCTURE_MAPPING_ID, clientParams.getInfraMappingId(), APP_NAME, clientParams.getAppName(), SLOT_NAME,
        clientParams.getSlotName());

    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(
        PerpetualTaskType.AZURE_WEB_APP_INSTANCE_SYNC, accountId, clientContext, schedule, false, "");
  }

  private Set<AzureWebAppInstanceInfo> getWebAppInstancesInfo(String appId, String infraMappingId) {
    List<Instance> instances = instanceService.getInstancesForAppAndInframapping(appId, infraMappingId);
    return emptyIfNull(instances)
        .stream()
        .map(Instance::getInstanceInfo)
        .filter(AzureWebAppInstanceInfo.class ::isInstance)
        .map(AzureWebAppInstanceInfo.class ::cast)
        .collect(Collectors.toSet());
  }
}
