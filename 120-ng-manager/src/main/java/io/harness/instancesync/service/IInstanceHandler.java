package io.harness.instancesync.service;

import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.InstanceSyncFlowType;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;

public interface IInstanceHandler {
  <T, O extends InfrastructureMapping> void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo);

  void syncInstances(String appId, String infraMappingId, InstanceSyncFlowType instanceSyncFlowType);
}
