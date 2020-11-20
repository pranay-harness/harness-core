package software.wings.service;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.AzureVMSSInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AzureVMSSInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AzureVMSSDeploymentKey;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureVMSSInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  private static final String VMSS_ID = "vmssId";

  @Inject private InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Set<String> vmssIds = getVMSSIds(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    String accountId = infrastructureMapping.getAccountId();
    return createPerpetualTasksForVMSSIds(
        vmssIds, accountId, infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    String accountId = infrastructureMapping.getAccountId();
    String appId = infrastructureMapping.getAppId();
    String infraMappingId = infrastructureMapping.getUuid();

    Set<String> existingVMSSIds = existingPerpetualTasks.stream()
                                      .map(task -> task.getClientContext().getClientParams())
                                      .map(params -> params.get(VMSS_ID))
                                      .collect(Collectors.toSet());

    Set<String> newDeploymentVMSSIds = deploymentSummaries.stream()
                                           .map(DeploymentSummary::getAzureVMSSDeploymentKey)
                                           .map(AzureVMSSDeploymentKey::getVmssId)
                                           .collect(Collectors.toSet());

    Set<String> newVMSSIds = Sets.difference(newDeploymentVMSSIds, existingVMSSIds);

    return createPerpetualTasksForVMSSIds(newVMSSIds, accountId, appId, infraMappingId);
  }

  private List<String> createPerpetualTasksForVMSSIds(
      Set<String> vmssIds, String accountId, String appId, String infraMappingId) {
    return vmssIds.stream()
        .map(vmssId
            -> AzureVMSSInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(appId)
                   .infraMappingId(infraMappingId)
                   .vmssId(vmssId)
                   .build())
        .map(params -> create(accountId, params))
        .collect(Collectors.toList());
  }

  private String create(String accountId, AzureVMSSInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> paramMap = ImmutableMap.of(HARNESS_APPLICATION_ID, clientParams.getAppId(),
        INFRASTRUCTURE_MAPPING_ID, clientParams.getInfraMappingId(), VMSS_ID, clientParams.getVmssId());
    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramMap).build();
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();
    return perpetualTaskService.createTask(
        PerpetualTaskType.AZURE_VMSS_INSTANCE_SYNC, accountId, clientContext, schedule, false, "");
  }

  private Set<String> getVMSSIds(String appId, String infraMappingId) {
    List<Instance> instances = instanceService.getInstancesForAppAndInframapping(appId, infraMappingId);
    return emptyIfNull(instances)
        .stream()
        .map(Instance::getInstanceInfo)
        .filter(AzureVMSSInstanceInfo.class ::isInstance)
        .map(AzureVMSSInstanceInfo.class ::cast)
        .map(AzureVMSSInstanceInfo::getVmssId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
