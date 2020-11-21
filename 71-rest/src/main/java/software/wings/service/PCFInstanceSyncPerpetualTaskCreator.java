package software.wings.service;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.perpetualtask.PerpetualTaskType.PCF_INSTANCE_SYNC;
import static io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClient.PCF_APPLICATION_NAME;

import static software.wings.service.InstanceSyncConstants.HARNESS_ACCOUNT_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static java.util.stream.Collectors.toSet;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.service.intfc.instance.InstanceService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
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
public class PCFInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;

  public String create(String accountId, PcfInstanceSyncPerpetualTaskClientParams clientParams) {
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

    return perpetualTaskService.createTask(PCF_INSTANCE_SYNC, accountId, clientContext, schedule, false, "");
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTaskRecords, InfrastructureMapping infrastructureMapping) {
    String appId = deploymentSummaries.iterator().next().getAppId();
    String infrastructureMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    String accountId = deploymentSummaries.iterator().next().getAccountId();

    Set<String> applicationsWithPerpetualTaskAlreadyPresent = getApplicationNames(existingPerpetualTaskRecords);

    Set<String> applicationsOfNewDeployment =
        deploymentSummaries.stream().map(PCFInstanceSyncPerpetualTaskCreator::getApplicationName).collect(toSet());

    Sets.SetView<String> applicationsToCreatePerpetualTasksFor =
        Sets.difference(applicationsOfNewDeployment, applicationsWithPerpetualTaskAlreadyPresent);

    return createPerpetualTasksForPcfApplications(
        appId, infrastructureMappingId, accountId, applicationsToCreatePerpetualTasksFor);
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

    return createPerpetualTasksForPcfApplications(infrastructureMapping.getAppId(), infrastructureMapping.getUuid(),
        infrastructureMapping.getAccountId(), applicationNames);
  }

  private List<String> createPerpetualTasksForPcfApplications(
      String appId, String infraMappingId, String accountId, Set<String> applicationNames) {
    List<String> perpetualTaskIds = new ArrayList<>();
    for (String applicationName : applicationNames) {
      PcfInstanceSyncPerpetualTaskClientParams pcfInstanceSyncParams =
          PcfInstanceSyncPerpetualTaskClientParams.builder()
              .accountId(accountId)
              .appId(appId)
              .inframappingId(infraMappingId)
              .applicationName(applicationName)
              .build();
      perpetualTaskIds.add(create(accountId, pcfInstanceSyncParams));
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
