package io.harness.instancesync.service;

import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.InstanceSyncFlowType;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;

public interface IInstanceHandler {
  void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo);

  void syncInstances(String appId, String infraMappingId, InstanceSyncFlowType instanceSyncFlowType);
}
