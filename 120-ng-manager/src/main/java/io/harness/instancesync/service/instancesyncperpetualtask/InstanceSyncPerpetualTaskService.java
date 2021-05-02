package io.harness.instancesync.service.instancesyncperpetualtask;

import io.harness.instancesync.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.entity.DeploymentSummary;

public interface InstanceSyncPerpetualTaskService {
  void createPerpetualTasks(InfrastructureMapping infrastructureMapping);

  void createPerpetualTasksForNewDeployment(
      InfrastructureMapping infrastructureMapping, DeploymentSummary deploymentSummary);

  void deletePerpetualTasks(InfrastructureMapping infrastructureMapping);

  void deletePerpetualTasks(String accountId, String infrastructureMappingId);

  void resetPerpetualTask(String accountId, String perpetualTaskId);

  void deletePerpetualTask(String accountId, String infrastructureMappingId, String perpetualTaskId);

  boolean isInstanceSyncByPerpetualTaskEnabled(InfrastructureMapping infrastructureMapping);
}
