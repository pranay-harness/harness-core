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
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.SpotinstAmiInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.SpotinstAmiDeploymentKey;
import software.wings.service.intfc.instance.InstanceService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
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
public class SpotinstAmiInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  public static final String ELASTIGROUP_ID = "elastigroupId";

  @Inject InstanceService instanceService;
  @Inject PerpetualTaskService perpetualTaskService;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    final Set<String> elastigroupIds =
        getElastigroupIds(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    final String accountId = infrastructureMapping.getAccountId();

    return createPerpetualTasksForElastigroupIds(
        elastigroupIds, accountId, infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    final String accountId = deploymentSummaries.iterator().next().getAccountId();
    final String appId = deploymentSummaries.iterator().next().getAppId();
    final String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();

    final Set<String> existingElastigroupIds =
        existingPerpetualTasks.stream()
            .map(task -> task.getClientContext().getClientParams())
            .map(params -> params.get(SpotinstAmiInstanceSyncPerpetualTaskClient.ELASTIGROUP_ID))
            .collect(Collectors.toSet());
    final Set<String> newDeploymentElastigroupIds = deploymentSummaries.stream()
                                                        .map(DeploymentSummary::getSpotinstAmiDeploymentKey)
                                                        .map(SpotinstAmiDeploymentKey::getElastigroupId)
                                                        .collect(Collectors.toSet());
    final Set<String> newElastigroupIds = Sets.difference(newDeploymentElastigroupIds, existingElastigroupIds);

    return createPerpetualTasksForElastigroupIds(newElastigroupIds, accountId, appId, infraMappingId);
  }

  private List<String> createPerpetualTasksForElastigroupIds(
      Set<String> elastigroupIds, String accountId, String appId, String infraMappingId) {
    return elastigroupIds.stream()
        .map(elastigroupId
            -> SpotinstAmiInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(appId)
                   .inframappingId(infraMappingId)
                   .elastigroupId(elastigroupId)
                   .build())
        .map(clientParams -> create(accountId, clientParams))
        .collect(Collectors.toList());
  }

  private Set<String> getElastigroupIds(String appId, String infraMappingId) {
    final List<Instance> instances = instanceService.getInstancesForAppAndInframapping(appId, infraMappingId);
    return emptyIfNull(instances)
        .stream()
        .map(Instance::getInstanceInfo)
        .filter(SpotinstAmiInstanceInfo.class ::isInstance)
        .map(SpotinstAmiInstanceInfo.class ::cast)
        .map(SpotinstAmiInstanceInfo::getElastigroupId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private String create(String accountId, SpotinstAmiInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> paramMap = ImmutableMap.of(HARNESS_APPLICATION_ID, clientParams.getAppId(),
        INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId(), ELASTIGROUP_ID, clientParams.getElastigroupId());

    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(
        PerpetualTaskType.SPOT_INST_AMI_INSTANCE_SYNC, accountId, clientContext, schedule, false, "");
  }
}
