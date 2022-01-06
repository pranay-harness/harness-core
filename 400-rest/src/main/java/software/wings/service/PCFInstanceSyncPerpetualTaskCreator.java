/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.perpetualtask.PerpetualTaskType.PCF_INSTANCE_SYNC;
import static io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClient.PCF_APPLICATION_NAME;

import static software.wings.service.InstanceSyncConstants.HARNESS_ACCOUNT_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static java.util.stream.Collectors.toSet;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;

import com.google.common.collect.Sets;
import com.google.protobuf.util.Durations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
public class PCFInstanceSyncPerpetualTaskCreator extends AbstractInstanceSyncPerpetualTaskCreator {
  public String create(PcfInstanceSyncPerpetualTaskClientParams clientParams, InfrastructureMapping infraMapping) {
    Map<String, String> paramMap = new HashMap<>();
    paramMap.put(HARNESS_ACCOUNT_ID, clientParams.getAccountId());
    paramMap.put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId());
    paramMap.put(HARNESS_APPLICATION_ID, clientParams.getAppId());
    paramMap.put(PCF_APPLICATION_NAME, clientParams.getApplicationName());

    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(PCF_INSTANCE_SYNC, infraMapping.getAccountId(), clientContext, schedule,
        false, getTaskDescription(infraMapping));
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTaskRecords, InfrastructureMapping infrastructureMapping) {
    Set<String> applicationsWithPerpetualTaskAlreadyPresent = getApplicationNames(existingPerpetualTaskRecords);

    Set<String> applicationsOfNewDeployment =
        deploymentSummaries.stream().map(PCFInstanceSyncPerpetualTaskCreator::getApplicationName).collect(toSet());

    Sets.SetView<String> applicationsToCreatePerpetualTasksFor =
        Sets.difference(applicationsOfNewDeployment, applicationsWithPerpetualTaskAlreadyPresent);

    return createPerpetualTasksForPcfApplications(applicationsToCreatePerpetualTasksFor, infrastructureMapping);
  }

  private Set<String> getApplicationNames(List<PerpetualTaskRecord> perpetualTaskRecords) {
    return emptyIfNull(perpetualTaskRecords)
        .stream()
        .map(perpetualTaskRecord -> perpetualTaskRecord.getClientContext().getClientParams().get(PCF_APPLICATION_NAME))
        .collect(toSet());
  }

  private static String getApplicationName(DeploymentSummary deploymentSummary) {
    return ((PcfDeploymentInfo) deploymentSummary.getDeploymentInfo()).getApplicationName();
  }

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Set<String> applicationNames =
        getApplicationNames(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());

    return createPerpetualTasksForPcfApplications(applicationNames, infrastructureMapping);
  }

  private List<String> createPerpetualTasksForPcfApplications(
      Set<String> applicationNames, InfrastructureMapping infrastructureMapping) {
    List<String> perpetualTaskIds = new ArrayList<>();
    for (String applicationName : applicationNames) {
      PcfInstanceSyncPerpetualTaskClientParams pcfInstanceSyncParams =
          PcfInstanceSyncPerpetualTaskClientParams.builder()
              .accountId(infrastructureMapping.getAccountId())
              .appId(infrastructureMapping.getAppId())
              .inframappingId(infrastructureMapping.getUuid())
              .applicationName(applicationName)
              .build();
      perpetualTaskIds.add(create(pcfInstanceSyncParams, infrastructureMapping));
    }

    return perpetualTaskIds;
  }

  private Set<String> getApplicationNames(String appId, String infraMappingId) {
    List<Instance> instanceList = instanceService.getInstancesForAppAndInframapping(appId, infraMappingId);
    return instanceList.stream().map(PCFInstanceSyncPerpetualTaskCreator::getApplicationName).collect(toSet());
  }

  private static String getApplicationName(Instance instance) {
    PcfInstanceInfo instanceInfo = (PcfInstanceInfo) instance.getInstanceInfo();
    return instanceInfo.getPcfApplicationName();
  }
}
