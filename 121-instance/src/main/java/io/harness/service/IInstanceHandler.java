package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.deploymentinfo.DeploymentInfo;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.models.InstanceSyncFlowType;
import io.harness.models.RollbackInfo;
import io.harness.pms.contracts.ambiance.Ambiance;

@OwnedBy(HarnessTeam.DX)
public interface IInstanceHandler {
  void handleNewDeployment(DeploymentSummary deploymentSummary, RollbackInfo rollbackInfo);

  void syncInstances(String accountId, String orgId, String projectId, String infrastructureMappingId,
      InstanceSyncFlowType instanceSyncFlowType);

  InfrastructureMapping getInfrastructureMapping(Ambiance ambiance);

  // Every handler needs to return the deploymentInfo object acc to the inframapping type they handle
  DeploymentInfo getDeploymentInfo(Ambiance ambiance);
}
