/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.AwsSshPTClientParams;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.util.Durations;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class AwsSshInstanceSyncPerpetualTaskCreator extends AbstractInstanceSyncPerpetualTaskCreator {
  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    return createPerpetualTaskInternal(infrastructureMapping);
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    if (isEmpty(existingPerpetualTasks)) {
      return createPerpetualTaskInternal(infrastructureMapping);
    }
    existingPerpetualTasks.stream().distinct().forEach(
        task -> perpetualTaskService.resetTask(infrastructureMapping.getAccountId(), task.getUuid(), null));
    return emptyList();
  }

  private List<String> create(AwsSshPTClientParams clientParams, InfrastructureMapping infraMapping) {
    Map<String, String> clientParamMap = ImmutableMap.of(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID,
        clientParams.getInframappingId(), InstanceSyncConstants.HARNESS_APPLICATION_ID, clientParams.getAppId());

    PerpetualTaskClientContext clientContext =
        PerpetualTaskClientContext.builder().clientParams(clientParamMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                                         .build();
    return singletonList(perpetualTaskService.createTask(PerpetualTaskType.AWS_SSH_INSTANCE_SYNC,
        infraMapping.getAccountId(), clientContext, schedule, false, getTaskDescription(infraMapping)));
  }

  private List<String> createPerpetualTaskInternal(InfrastructureMapping infrastructureMapping) {
    AwsSshPTClientParams params = AwsSshPTClientParams.builder()
                                      .appId(infrastructureMapping.getAppId())
                                      .inframappingId(infrastructureMapping.getUuid())
                                      .build();
    return create(params, infrastructureMapping);
  }
}
