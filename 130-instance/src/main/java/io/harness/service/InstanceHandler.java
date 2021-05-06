package io.harness.service;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.OnDemandRollbackInfo;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.service.infrastructuremapping.InfrastructureMappingService;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.instance.InstanceSyncFlow;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;

public abstract class InstanceHandler<T, O extends InfrastructureMapping>
    implements IInstanceHandler<T, O>, IInstanceSyncByPerpetualTaskhandler {
  @Inject protected InfrastructureMappingService infrastructureMappingService;

  protected abstract void validateDeploymentInfo(DeploymentSummary deploymentSummary);

  protected abstract Multimap<T, Instance> createDeploymentInstanceMap(DeploymentSummary deploymentSummary);

  protected abstract O getDeploymentInfrastructureMapping(DeploymentSummary deploymentSummary);

  protected abstract void syncInstancesInternal(O inframapping, Multimap<T, Instance> deploymentInstanceMap,
      DeploymentSummary newDeploymentSummary, boolean rollback, DelegateResponseData responseData,
      InstanceSyncFlow instanceSyncFlow);

  public void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    if (deploymentSummary == null) {
      return;
    }

    validateDeploymentInfo(deploymentSummary);

    // Check use of generics here
    Multimap<T, Instance> deploymentToInstanceMap = createDeploymentInstanceMap(deploymentSummary);

    //

    O inframapping = getDeploymentInfrastructureMapping(deploymentSummary);

    //

    syncInstancesInternal(
        inframapping, deploymentToInstanceMap, deploymentSummary, rollback, null, InstanceSyncFlow.NEW_DEPLOYMENT);
  }

  public void syncInstancesInternal() {
    // fill in the deployment details VS instance map from DB
    // fetch instances from DB using inframapping id
    //        loadContainerSvcNameInstanceMap(containerInfraMapping, containerMetadataInstanceMap);
  }
}