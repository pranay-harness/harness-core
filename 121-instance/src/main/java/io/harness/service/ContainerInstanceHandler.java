package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.DeploymentInfo;
import io.harness.dto.deploymentinfo.RollbackInfo;
import io.harness.dto.infrastructureMapping.DirectKubernetesInfrastructureMapping;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.entity.InstanceSyncFlowType;
import io.harness.pms.contracts.ambiance.Ambiance;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.instance.Status;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.DX)
public class ContainerInstanceHandler
    extends InstanceHandler<ContainerMetadata, DirectKubernetesInfrastructureMapping> {
  @Override
  public FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync() {
    return null;
  }

  @Override
  public IInstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator() {
    return null;
  }

  @Override
  public Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    return null;
  }

  @Override
  protected void validateDeploymentInfo(DeploymentSummary deploymentSummary) {}

  @Override
  protected Multimap<ContainerMetadata, Instance> createDeploymentInstanceMap(DeploymentSummary deploymentSummary) {
    Multimap<ContainerMetadata, Instance> deploymentMap = ArrayListMultimap.create();
    return deploymentMap;
  }

  @Override
  protected void syncInstancesInternal(DirectKubernetesInfrastructureMapping inframapping,
      Multimap<ContainerMetadata, Instance> deploymentInstanceMap, DeploymentSummary newDeploymentSummary,
      RollbackInfo rollbackInfo, DelegateResponseData responseData, InstanceSyncFlowType instanceSyncFlow) {}

  @Override
  protected DirectKubernetesInfrastructureMapping validateAndReturnInfrastructureMapping(
      InfrastructureMapping infrastructureMapping) {
    return null;
  }

  @Override
  protected void validatePerpetualTaskDelegateResponse(DelegateResponseData response) {}

  @Override
  protected String getInstanceUniqueIdentifier(Instance instance) {
    return null;
  }

  @Override
  protected InfrastructureMapping getInfrastructureMappingByType(
      Ambiance ambiance, ServiceOutcome serviceOutcome, InfrastructureOutcome infrastructureOutcome) {
    return null;
  }

  @Override
  public DeploymentInfo getDeploymentInfo(Ambiance ambiance) {
    return null;
  }
}
