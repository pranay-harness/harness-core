package io.harness.service;

import io.harness.entities.DeploymentSummary;
import io.harness.entities.deploymentinfo.DeploymentInfo;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pojo.InstanceSyncFlowType;
import io.harness.pojo.RollbackInfo;

public interface IInstanceHandler {
  void handleNewDeployment(DeploymentSummary deploymentSummary, RollbackInfo rollbackInfo);

  void syncInstances(String accountId, String orgId, String projectId, String infrastructureMappingId,
      InstanceSyncFlowType instanceSyncFlowType);

  InfrastructureMapping getInfrastructureMapping(Ambiance ambiance);

  DeploymentInfo getDeploymentInfo(Ambiance ambiance);
}
