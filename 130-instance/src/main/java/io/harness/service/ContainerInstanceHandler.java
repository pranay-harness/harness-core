package io.harness.service;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.OnDemandRollbackInfo;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.entity.InstanceSyncFlowType;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.instance.InstanceSyncFlow;
import software.wings.service.impl.instance.Status;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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

  @Override
  protected void validateDeploymentInfo(DeploymentSummary deploymentSummary) {}

  @Override
  protected <T> Multimap<T, Instance> createDeploymentInstanceMap(DeploymentSummary deploymentSummary) {
    Multimap<ContainerMetadata, Instance> deploymentMap = ArrayListMultimap.create();
    return (Multimap<T, Instance>) deploymentMap;
  }

  @Override
  protected <T extends InfrastructureMapping> T getDeploymentInfrastructureMapping(
      DeploymentSummary deploymentSummary) {
    return null;
  }

  @Override
  protected <T extends InfrastructureMapping, O> void syncInstancesInternal(T inframapping,
      Multimap<O, Instance> deploymentInstanceMap, DeploymentSummary newDeploymentSummary, boolean rollback,
      DelegateResponseData responseData, InstanceSyncFlow instanceSyncFlow) {}
}
