/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.AzureVMSSInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AzureVMSSInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AzureVMSSDeploymentKey;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.protobuf.util.Durations;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
public class AzureVMSSInstanceSyncPerpetualTaskCreator extends AbstractInstanceSyncPerpetualTaskCreator {
  private static final String VMSS_ID = "vmssId";

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Set<String> vmssIds = getVMSSIds(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    String accountId = infrastructureMapping.getAccountId();
    return createPerpetualTasksForVMSSIds(vmssIds, infrastructureMapping);
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    Set<String> existingVMSSIds = existingPerpetualTasks.stream()
                                      .map(task -> task.getClientContext().getClientParams())
                                      .map(params -> params.get(VMSS_ID))
                                      .collect(Collectors.toSet());

    Set<String> newDeploymentVMSSIds = deploymentSummaries.stream()
                                           .map(DeploymentSummary::getAzureVMSSDeploymentKey)
                                           .map(AzureVMSSDeploymentKey::getVmssId)
                                           .collect(Collectors.toSet());

    Set<String> newVMSSIds = Sets.difference(newDeploymentVMSSIds, existingVMSSIds);

    return createPerpetualTasksForVMSSIds(newVMSSIds, infrastructureMapping);
  }

  private List<String> createPerpetualTasksForVMSSIds(
      Set<String> vmssIds, InfrastructureMapping infrastructureMapping) {
    return vmssIds.stream()
        .map(vmssId
            -> AzureVMSSInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(infrastructureMapping.getAppId())
                   .infraMappingId(infrastructureMapping.getUuid())
                   .vmssId(vmssId)
                   .build())
        .map(params -> create(params, infrastructureMapping))
        .collect(Collectors.toList());
  }

  private String create(
      AzureVMSSInstanceSyncPerpetualTaskClientParams clientParams, InfrastructureMapping infraMapping) {
    Map<String, String> paramMap = ImmutableMap.of(HARNESS_APPLICATION_ID, clientParams.getAppId(),
        INFRASTRUCTURE_MAPPING_ID, clientParams.getInfraMappingId(), VMSS_ID, clientParams.getVmssId());

    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramMap).build();
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();
    return perpetualTaskService.createTask(PerpetualTaskType.AZURE_VMSS_INSTANCE_SYNC, infraMapping.getAccountId(),
        clientContext, schedule, false, getTaskDescription(infraMapping));
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
