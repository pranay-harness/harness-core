package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.InfrastructureMapping;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface IInstanceSyncPerpetualTaskCreator {
  List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping);

  List<String> createPerpetualTasksForNewDeployment(DeploymentSummary deploymentSummary,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping);
}
