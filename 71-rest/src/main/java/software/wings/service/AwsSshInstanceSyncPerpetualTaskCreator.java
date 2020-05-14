package software.wings.service;

import com.google.inject.Inject;

import io.harness.perpetualtask.AwsSshPTClientParams;
import io.harness.perpetualtask.instancesync.AwsSshPerpetualTaskServiceClient;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class AwsSshInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject private AwsSshPerpetualTaskServiceClient perpetualTaskServiceClient;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    return Arrays.asList(createPerpetualTaskInternal(
        infrastructureMapping.getAccountId(), infrastructureMapping.getAppId(), infrastructureMapping.getUuid()));
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    return createPerpetualTasks(infrastructureMapping);
  }

  private String createPerpetualTaskInternal(String accountId, String appId, String infraMappingId) {
    AwsSshPTClientParams params = AwsSshPTClientParams.builder().appId(appId).inframappingId(infraMappingId).build();
    return perpetualTaskServiceClient.create(accountId, params);
  }
}
