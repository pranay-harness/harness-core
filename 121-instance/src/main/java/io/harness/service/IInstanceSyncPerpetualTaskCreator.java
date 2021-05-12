package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;

@OwnedBy(HarnessTeam.DX)
public interface IInstanceSyncPerpetualTaskCreator<T extends InfrastructureMapping> {
  String createPerpetualTaskForNewDeployment(DeploymentSummary deploymentSummary, T infrastructureMapping);
}
