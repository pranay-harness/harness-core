package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.RollbackInfo;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.entity.InstanceSyncFlowType;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.repository.infrastructuremapping.InfrastructureMappingRepository;

import software.wings.beans.infrastructure.instance.Instance;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
public abstract class InstanceHandler<T, O extends InfrastructureMapping>
    implements IInstanceHandler<T, O>, IInstanceSyncByPerpetualTaskhandler {
  @Inject protected InfrastructureMappingRepository infrastructureMappingRepository;
  @Inject protected OutcomeService outcomeService;

  protected abstract void validateDeploymentInfo(DeploymentSummary deploymentSummary);

  protected abstract Multimap<T, Instance> createDeploymentInstanceMap(DeploymentSummary deploymentSummary);

  protected abstract void syncInstancesInternal(O inframapping, Multimap<T, Instance> deploymentInstanceMap,
      DeploymentSummary newDeploymentSummary, RollbackInfo rollbackInfo, DelegateResponseData responseData,
      InstanceSyncFlowType instanceSyncFlow);

  protected abstract O validateAndReturnInfrastructureMapping(InfrastructureMapping infrastructureMapping);

  protected abstract void validatePerpetualTaskDelegateResponse(DelegateResponseData response);

  protected abstract String getInstanceUniqueIdentifier(Instance instance);

  // Get concrete infrastructure mapping object based on infrastructure mapping type
  protected abstract InfrastructureMapping getInfrastructureMappingByType(
      Ambiance ambiance, ServiceOutcome serviceOutcome, InfrastructureOutcome infrastructureOutcome);

  public final InfrastructureMapping getInfrastructureMapping(Ambiance ambiance) {
    ServiceOutcome serviceOutcome = getServiceOutcomeFromAmbiance(ambiance);
    InfrastructureOutcome infrastructureOutcome = getInfrastructureOutcomeFromAmbiance(ambiance);

    // Set connectorRef + specific infrastructure mapping fields
    InfrastructureMapping infrastructureMapping =
        getInfrastructureMappingByType(ambiance, serviceOutcome, infrastructureOutcome);

    // Set common parent class fields here
    infrastructureMapping.setAccountIdentifier(AmbianceHelper.getAccountId(ambiance));
    infrastructureMapping.setOrgIdentifier(AmbianceHelper.getOrgIdentifier(ambiance));
    infrastructureMapping.setProjectIdentifier(AmbianceHelper.getProjectIdentifier(ambiance));
    infrastructureMapping.setServiceId(serviceOutcome.getIdentifier());
    infrastructureMapping.setInfrastructureMappingType(infrastructureOutcome.getType());
    infrastructureMapping.setEnvId(infrastructureOutcome.getEnvironment().getIdentifier());

    // TODO set deployment type and id

    return infrastructureMapping;
  }

  public final void handleNewDeployment(DeploymentSummary deploymentSummary, RollbackInfo rollbackInfo) {
    if (deploymentSummary == null) {
      return;
    }

    validateDeploymentInfo(deploymentSummary);

    // Check use of generics here
    Multimap<T, Instance> deploymentToInstanceMap = createDeploymentInstanceMap(deploymentSummary);

    //

    O infrastructureMapping = getDeploymentInfrastructureMapping(deploymentSummary.getAccountIdentifier(),
        deploymentSummary.getOrgIdentifier(), deploymentSummary.getProjectIdentifier(),
        deploymentSummary.getInfrastructureMappingId());

    syncInstancesInternal(infrastructureMapping, deploymentToInstanceMap, deploymentSummary, rollbackInfo, null,
        InstanceSyncFlowType.NEW_DEPLOYMENT);
  }

  public final void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    O infrastructureMappingDetails = validateAndReturnInfrastructureMapping(infrastructureMapping);

    validatePerpetualTaskDelegateResponse(response);

    syncInstancesInternal(infrastructureMappingDetails, ArrayListMultimap.create(), null,
        RollbackInfo.builder().isRollback(false).build(), response, InstanceSyncFlowType.PERPETUAL_TASK);
  }

  public final void syncInstances(String accountId, String orgId, String projectId, String infrastructureMappingId,
      InstanceSyncFlowType instanceSyncFlowType) {
    O infrastructureMapping = getDeploymentInfrastructureMapping(accountId, orgId, projectId, infrastructureMappingId);

    syncInstancesInternal(infrastructureMapping, ArrayListMultimap.create(), null,
        RollbackInfo.builder().isRollback(false).build(), null, instanceSyncFlowType);
  }

  public void syncInstancesInternal() {
    // fill in the deployment details VS instance map from DB
    // fetch instances from DB using inframapping id
    //        loadContainerSvcNameInstanceMap(containerInfraMapping, containerMetadataInstanceMap);
  }

  protected final void createOrUpdateInstances(List<Instance> oldInstances, List<Instance> newInstances) {
    //    we delete the instance oldInstances - newInstances
    //    we create the instance newInstances - oldInstances
    // also update common instances with the newInstances details

    // Every instance will have a unique key
    // for k8s pods:  podInfo.getName() + podInfo.getNamespace() + getImageInStringFormat(podInfo)
    // We use this info to compare new and old instances
  }

  protected ServiceOutcome getServiceOutcomeFromAmbiance(Ambiance ambiance) {
    return (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
  }

  protected InfrastructureOutcome getInfrastructureOutcomeFromAmbiance(Ambiance ambiance) {
    return (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));
  }

  // ---------------------------- PRIVATE METHODS ---------------------------

  private O getDeploymentInfrastructureMapping(
      String accountId, String orgId, String projectId, String infrastructureMappingId) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingRepository.get(accountId, orgId, projectId, infrastructureMappingId);
    return validateAndReturnInfrastructureMapping(infrastructureMapping);
  }
}