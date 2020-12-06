package software.wings.service;

import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomDeploymentInstanceSyncPTCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject PerpetualTaskService perpetualTaskService;
  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    return Arrays.asList(createPerpetualTask(infrastructureMapping));
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    if (EmptyPredicate.isNotEmpty(existingPerpetualTasks)) {
      if (existingPerpetualTasks.size() > 1) {
        log.error(format("More than 1 Custom Deployment Instance Sync Perpetual Tasks exist for InfraMappingId %s",
            infrastructureMapping.getUuid()));
      }
      // To allow for updation on script on deployment, task is reset so that some other delegate can pick up the task
      // and fetch the task params
      existingPerpetualTasks.forEach(
          task -> perpetualTaskService.resetTask(infrastructureMapping.getAccountId(), task.getUuid(), null));
      return existingPerpetualTasks.stream().map(PerpetualTaskRecord::getUuid).collect(Collectors.toList());
    }
    return Arrays.asList(createPerpetualTask(infrastructureMapping));
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
        infraMapping.getAccountId(), clientContext, schedule, false, "Instance Sync Task For Custom Deployment");
  }
}
