package software.wings.service;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;

import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsAmiInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject InstanceService instanceService;
  @Inject AwsAmiInstanceSyncPerpetualTaskClient awsAmiInstanceSyncPerpetualTaskClient;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    final Set<String> asgNames = getAsgNames(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    final String accountId = infrastructureMapping.getAccountId();
    return createPerpetualTasks(asgNames, accountId, infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
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

    return createPerpetualTasks(asgEligibleForPerpetualTask, accountId, appId, infraMappingId);
  }

  private Set<String> asgWithExistingPerpetualTasks(List<PerpetualTaskRecord> existingPerpetualTasks) {
    return existingPerpetualTasks.stream()
        .map(record -> record.getClientContext().getClientParams().get(AwsAmiInstanceSyncPerpetualTaskClient.ASG_NAME))
        .collect(Collectors.toSet());
  }

  private List<String> createPerpetualTasks(
      Set<String> asgNames, String accountId, String appId, String infraMappingId) {
    return asgNames.stream()
        .map(asgName
            -> AwsAmiInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(appId)
                   .asgName(asgName)
                   .inframappingId(infraMappingId)
                   .build())
        .map(params -> awsAmiInstanceSyncPerpetualTaskClient.create(accountId, params))
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
}
