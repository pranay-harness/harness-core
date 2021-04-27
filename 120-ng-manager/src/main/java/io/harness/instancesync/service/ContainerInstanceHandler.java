package io.harness.instancesync.service;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.InstanceSyncFlowType;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;

import software.wings.service.impl.instance.Status;

public class ContainerInstanceHandler extends InstanceHandler {
  @Override
  public void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {}

  @Override
  public void syncInstances(String appId, String infraMappingId, InstanceSyncFlowType instanceSyncFlowType) {}

  @Override
  public FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync() {
    return null;
  }

  @Override
  public IInstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator() {
    return null;
  }

  @Override
  public void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response) {}

  @Override
  public Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    return null;
  }
}
