package io.harness.service;

import io.harness.entities.DeploymentSummary;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import java.util.List;

public interface IInstanceSyncPerpetualTaskCreator {
  List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping);

  List<String> createPerpetualTasksForNewDeployment(DeploymentSummary deploymentSummary,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping);
}
