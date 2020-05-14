package software.wings.service.impl.instance;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import java.util.List;

public interface InstanceSyncPerpetualTaskService {
  void createPerpetualTasks(InfrastructureMapping infrastructureMapping);

  void createPerpetualTasksForNewDeployment(
      InfrastructureMapping infrastructureMapping, List<DeploymentSummary> deploymentSummaries);

  void deletePerpetualTasks(InfrastructureMapping infrastructureMapping);

  void deletePerpetualTasks(String accountId, String infrastructureMappingId);

  void resetPerpetualTask(String accountId, String perpetualTaskId);

  void deletePerpetualTask(String accountId, String infrastructureMappingId, String perpetualTaskId);
}
