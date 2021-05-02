package io.harness.instancesync.service;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.instancesync.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.instancesync.service.infrastructuremapping.InfrastructureMappingService;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.instance.InstanceSyncFlow;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;

public abstract class InstanceHandler implements IInstanceHandler, IInstanceSyncByPerpetualTaskhandler {
  @Inject protected InfrastructureMappingService infrastructureMappingService;

  protected abstract void validateDeploymentInfo(DeploymentSummary deploymentSummary);

  protected abstract <T> Multimap<T, Instance> createDeploymentInstanceMap(DeploymentSummary deploymentSummary);

  protected abstract <T extends InfrastructureMapping> T getDeploymentInfrastructureMapping(
      DeploymentSummary deploymentSummary);

  protected abstract <T extends InfrastructureMapping, O> void syncInstancesInternal(T inframapping,
      Multimap<O, Instance> deploymentInstanceMap, DeploymentSummary newDeploymentSummary, boolean rollback,
      DelegateResponseData responseData, InstanceSyncFlow instanceSyncFlow);

  public <T, O extends InfrastructureMapping> void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    if (deploymentSummary == null) {
      return;
    }

    validateDeploymentInfo(deploymentSummary);

    // Check use of generics here
    Multimap<T, Instance> deploymentToInstanceMap = createDeploymentInstanceMap(deploymentSummary);

    O inframapping = getDeploymentInfrastructureMapping(deploymentSummary);

    syncInstancesInternal(
        inframapping, deploymentToInstanceMap, deploymentSummary, rollback, null, InstanceSyncFlow.NEW_DEPLOYMENT);
  }

  public void syncInstancesInternal() {
    // fill in the deployment details VS instance map from DB
    // fetch instances from DB using inframapping id
    //        loadContainerSvcNameInstanceMap(containerInfraMapping, containerMetadataInstanceMap);
  }
}