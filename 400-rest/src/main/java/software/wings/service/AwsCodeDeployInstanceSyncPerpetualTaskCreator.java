/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.perpetualtask.PerpetualTaskType.AWS_CODE_DEPLOY_INSTANCE_SYNC;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.AwsCodeDeployInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import com.google.protobuf.util.Durations;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
public class AwsCodeDeployInstanceSyncPerpetualTaskCreator extends AbstractInstanceSyncPerpetualTaskCreator {
  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    return singletonList(createPerpetualTask(infrastructureMapping));
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    String appId = deploymentSummaries.iterator().next().getAppId();

    boolean taskAlreadyExists =
        existingPerpetualTasks.stream()
            .map(record -> record.getClientContext().getClientParams().get(HARNESS_APPLICATION_ID))
            .anyMatch(existingAppId -> existingAppId.equals(appId));

    return taskAlreadyExists ? emptyList() : createPerpetualTasks(infrastructureMapping);
  }

  private String createPerpetualTask(InfrastructureMapping infrastructureMapping) {
    AwsCodeDeployInstanceSyncPerpetualTaskClientParams clientParams =
        AwsCodeDeployInstanceSyncPerpetualTaskClientParams.builder()
            .appId(infrastructureMapping.getAppId())
            .inframmapingId(infrastructureMapping.getUuid())
            .build();

    return create(clientParams, infrastructureMapping);
  }

  private String create(
      AwsCodeDeployInstanceSyncPerpetualTaskClientParams clientParams, InfrastructureMapping infraMapping) {
    Map<String, String> paramsMap = new HashMap<>();
    paramsMap.put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframmapingId());
    paramsMap.put(HARNESS_APPLICATION_ID, clientParams.getAppId());

    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramsMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(AWS_CODE_DEPLOY_INSTANCE_SYNC, infraMapping.getAccountId(), clientContext,
        schedule, false, getTaskDescription(infraMapping));
  }
}
