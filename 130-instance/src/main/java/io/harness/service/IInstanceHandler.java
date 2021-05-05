package io.harness.service;

import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.OnDemandRollbackInfo;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.entity.InstanceSyncFlowType;

public interface IInstanceHandler {
  <T, O extends InfrastructureMapping> void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo);

  void syncInstances(String appId, String infraMappingId, InstanceSyncFlowType instanceSyncFlowType);
}
