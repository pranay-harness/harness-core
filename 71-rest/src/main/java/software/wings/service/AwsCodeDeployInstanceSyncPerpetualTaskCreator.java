package software.wings.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;

import com.google.inject.Inject;

import io.harness.perpetualtask.AwsCodeDeployInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.AwsCodeDeployInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsCodeDeployInstanceSyncPerpetualTaskCreator implements InstanceSyncPerpetualTaskCreator {
  @Inject AwsCodeDeployInstanceSyncPerpetualTaskClient perpetualTaskClient;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    return singletonList(createPerpetualTask(
        infrastructureMapping.getAccountId(), infrastructureMapping.getAppId(), infrastructureMapping.getUuid()));
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

  private String createPerpetualTask(String accountId, String appId, String infraMappingId) {
    AwsCodeDeployInstanceSyncPerpetualTaskClientParams clientParams =
        AwsCodeDeployInstanceSyncPerpetualTaskClientParams.builder()
            .appId(appId)
            .inframmapingId(infraMappingId)
            .build();

    return perpetualTaskClient.create(accountId, clientParams);
  }
}
