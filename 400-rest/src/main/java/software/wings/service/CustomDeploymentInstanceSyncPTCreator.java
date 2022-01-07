/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.util.Durations;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class CustomDeploymentInstanceSyncPTCreator extends AbstractInstanceSyncPerpetualTaskCreator {
  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    return asList(createPerpetualTask(infrastructureMapping));
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    if (isNotEmpty(existingPerpetualTasks)) {
      if (existingPerpetualTasks.size() > 1) {
        log.error(format("More than 1 Custom Deployment Instance Sync Perpetual Tasks exist for InfraMappingId %s",
            infrastructureMapping.getUuid()));
      }
      // To allow for updation on script on deployment, task is reset so that some other delegate can pick up the task
      // and fetch the task params
      existingPerpetualTasks.stream().distinct().forEach(
          task -> perpetualTaskService.resetTask(infrastructureMapping.getAccountId(), task.getUuid(), null));
      return Collections.emptyList();
    }
    return asList(createPerpetualTask(infrastructureMapping));
  }

  private String createPerpetualTask(InfrastructureMapping infraMapping) {
    final Map<String, String> paramMap =
        ImmutableMap.<String, String>builder()
            .put(InstanceSyncConstants.HARNESS_ACCOUNT_ID, infraMapping.getAccountId())
            .put(InstanceSyncConstants.HARNESS_APPLICATION_ID, infraMapping.getAppId())
            .put(InstanceSyncConstants.HARNESS_ENV_ID, infraMapping.getEnvId())
            .put(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, infraMapping.getUuid())
            .build();
    final PerpetualTaskClientContext clientContext =
        PerpetualTaskClientContext.builder().clientParams(paramMap).build();

    final PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                               .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                               .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                               .build();

    return perpetualTaskService.createTask(PerpetualTaskType.CUSTOM_DEPLOYMENT_INSTANCE_SYNC,
        infraMapping.getAccountId(), clientContext, schedule, false, getTaskDescription(infraMapping));
  }
}
