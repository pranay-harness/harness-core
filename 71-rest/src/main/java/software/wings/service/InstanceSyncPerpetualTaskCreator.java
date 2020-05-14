package software.wings.service;

import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;

import java.util.List;

public interface InstanceSyncPerpetualTaskCreator {
  List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping);

  List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping);
}
