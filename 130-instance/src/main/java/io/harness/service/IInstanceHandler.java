package io.harness.service;

import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.entity.DeploymentSummary;
import io.harness.entity.InstanceSyncFlowType;
import io.harness.entity.deploymentinfo.OnDemandRollbackInfo;

public interface IInstanceHandler {
  <T, O extends InfrastructureMapping> void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo);

  void syncInstances(String appId, String infraMappingId, InstanceSyncFlowType instanceSyncFlowType);
}
