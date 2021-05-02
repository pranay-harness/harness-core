package io.harness.instancesync.service;

import io.harness.instancesync.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import java.util.List;

public interface IInstanceSyncPerpetualTaskCreator {
  List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping);

  List<String> createPerpetualTasksForNewDeployment(DeploymentSummary deploymentSummary,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping);
}
