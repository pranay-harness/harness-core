package io.harness.service;

import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.RollbackInfo;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.entity.InstanceSyncFlowType;

public interface IInstanceHandler<T, O extends InfrastructureMapping> {
  void handleNewDeployment(DeploymentSummary deploymentSummary, RollbackInfo rollbackInfo);

  void syncInstances(String accountId, String orgId, String projectId, String infrastructureMappingId,
      InstanceSyncFlowType instanceSyncFlowType);
}
