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
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.protobuf.util.Durations;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
public class AwsAmiInstanceSyncPerpetualTaskCreator extends AbstractInstanceSyncPerpetualTaskCreator {
  public static final String ASG_NAME = "asgName";

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    final Set<String> asgNames = getAsgNames(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    final String accountId = infrastructureMapping.getAccountId();
    return createPerpetualTasks(asgNames, infrastructureMapping);
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    String appId = deploymentSummaries.iterator().next().getAppId();
    String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    String accountId = deploymentSummaries.iterator().next().getAccountId();

    final Set<String> asgWithExistingPerpetualTasks = asgWithExistingPerpetualTasks(existingPerpetualTasks);

    final Set<String> asgsFromNewDeployment = deploymentSummaries.stream()
                                                  .map(DeploymentSummary::getDeploymentInfo)
                                                  .map(AwsAutoScalingGroupDeploymentInfo.class ::cast)
                                                  .map(AwsAutoScalingGroupDeploymentInfo::getAutoScalingGroupName)
                                                  .collect(Collectors.toSet());

    SetView<String> asgEligibleForPerpetualTask = Sets.difference(asgsFromNewDeployment, asgWithExistingPerpetualTasks);

    return createPerpetualTasks(asgEligibleForPerpetualTask, infrastructureMapping);
  }

  private Set<String> asgWithExistingPerpetualTasks(List<PerpetualTaskRecord> existingPerpetualTasks) {
    return existingPerpetualTasks.stream()
        .map(record -> record.getClientContext().getClientParams().get(AwsAmiInstanceSyncPerpetualTaskClient.ASG_NAME))
        .collect(Collectors.toSet());
  }

  private List<String> createPerpetualTasks(Set<String> asgNames, InfrastructureMapping infrastructureMapping) {
    return asgNames.stream()
        .map(asgName
            -> AwsAmiInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(infrastructureMapping.getAppId())
                   .asgName(asgName)
                   .inframappingId(infrastructureMapping.getUuid())
                   .build())
        .map(params -> create(params, infrastructureMapping))
        .collect(Collectors.toList());
  }

  private Set<String> getAsgNames(String appId, String infraMappingId) {
    final List<Instance> instances = instanceService.getInstancesForAppAndInframapping(appId, infraMappingId);
    return emptyIfNull(instances)
        .stream()
        .map(Instance::getInstanceInfo)
        .filter(AutoScalingGroupInstanceInfo.class ::isInstance)
        .map(AutoScalingGroupInstanceInfo.class ::cast)
        .map(AutoScalingGroupInstanceInfo::getAutoScalingGroupName)
        .collect(Collectors.toSet());
  }

  private String create(AwsAmiInstanceSyncPerpetualTaskClientParams clientParams, InfrastructureMapping infraMapping) {
    Map<String, String> paramMap = ImmutableMap.of(HARNESS_APPLICATION_ID, clientParams.getAppId(),
        INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId(), ASG_NAME, clientParams.getAsgName());

    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(PerpetualTaskType.AWS_AMI_INSTANCE_SYNC, infraMapping.getAccountId(),
        clientContext, schedule, false, getTaskDescription(infraMapping));
  }
}
