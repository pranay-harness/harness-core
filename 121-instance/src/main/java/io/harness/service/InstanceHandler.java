package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.deploymentinfo.DeploymentInfo;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.entities.instance.Instance;
import io.harness.entities.instanceinfo.InstanceInfo;
import io.harness.models.InstanceHandlerKey;
import io.harness.models.InstanceSyncFlowType;
import io.harness.models.RollbackInfo;
import io.harness.models.ServerInstance;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.repositories.infrastructuremapping.InfrastructureMappingRepository;
import io.harness.repositories.instance.InstanceRepository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
public abstract class InstanceHandler<T extends InstanceHandlerKey, U extends InfrastructureMapping, V
                                          extends DeploymentInfo, X extends DelegateResponseData, Y
                                          extends InstanceInfo, Z extends ServerInstance>
    implements IInstanceHandler, IInstanceSyncByPerpetualTaskhandler<X>, IInstanceSyncPerpetualTaskCreator<U> {
  public static final String AUTO_SCALE = "AUTO_SCALE";

  @Inject protected InfrastructureMappingRepository infrastructureMappingRepository;
  @Inject protected OutcomeService outcomeService;
  @Inject private InstanceRepository instanceRepository;
  @Inject PerpetualTaskService perpetualTaskService;

  protected abstract V validateAndReturnDeploymentInfo(DeploymentSummary deploymentSummary);

  protected abstract Y validateAndReturnInstanceInfo(InstanceInfo instanceInfo);

  protected abstract Multimap<T, Instance> createDeploymentInstanceMap(DeploymentSummary deploymentSummary);

  protected abstract U validateAndReturnInfrastructureMapping(InfrastructureMapping infrastructureMapping);

  protected abstract void validatePerpetualTaskDelegateResponse(X response);

  protected abstract String getInstanceUniqueIdentifier(Instance instance);

  // This key acts as common key for logical handling of instances in the handler
  // Fetch it using deployment info
  protected abstract T getInstanceHandlerKey(V deploymentInfo);

  // Fetch instance handler key using instance info
  protected abstract T getInstanceHandlerKey(Y instanceInfo);

  // Fetch instance handler key from delegate response
  protected abstract T getInstanceHandlerKey(X delegateResponseData);

  protected abstract Y getInstanceInfo(Z serverInstance);

  protected abstract List<Z> getServerInstancesFromDelegateResponse(X delegateResponseData);

  protected abstract V getEmptyDeploymentInfoObject();

  protected abstract X executeDelegateSyncTaskToFetchServerInstances(U infrastructureMapping, T instanceHandlerKey);

  // Add custom deployment details to the client param map for perpetual task
  protected abstract void populateClientParamMapForPerpetualTask(
      Map<String, String> clientParamMap, T instanceHandlerKey);

  protected abstract String getPerpetualTaskType();

  // Override this method in case of specific custom instance build requirements
  protected void buildInstanceCustom(Instance.InstanceBuilder instanceBuilder, Z serverInstance) {
    return;
  }

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

    validateAndReturnDeploymentInfo(deploymentSummary);

    // Infrastructure mapping is already present in deployment summary
    U infrastructureMapping = validateAndReturnInfrastructureMapping(deploymentSummary.getInfrastructureMapping());

    syncInstancesInternal(
        infrastructureMapping, deploymentSummary, rollbackInfo, null, InstanceSyncFlowType.NEW_DEPLOYMENT);
  }

  public final void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, X response) {
    U infrastructureMappingDetails = validateAndReturnInfrastructureMapping(infrastructureMapping);

    validatePerpetualTaskDelegateResponse(response);

    syncInstancesInternal(infrastructureMappingDetails, null, RollbackInfo.builder().isRollback(false).build(),
        response, InstanceSyncFlowType.PERPETUAL_TASK);
  }

  public final void syncInstances(String accountId, String orgId, String projectId, String infrastructureMappingId,
      InstanceSyncFlowType instanceSyncFlowType) {
    U infrastructureMapping = getDeploymentInfrastructureMapping(accountId, orgId, projectId, infrastructureMappingId);

    syncInstancesInternal(
        infrastructureMapping, null, RollbackInfo.builder().isRollback(false).build(), null, instanceSyncFlowType);
  }

  public final String createPerpetualTaskForNewDeployment(
      DeploymentSummary deploymentSummary, T infrastructureMapping) {
    T instanceHandlerKey = getInstanceHandlerKey(validateAndReturnDeploymentInfo(deploymentSummary));

    Map<String, String> clientParamMap = prepareClientParamMapForPerpetualTask(deploymentSummary);
    populateClientParamMapForPerpetualTask(clientParamMap, instanceHandlerKey);

    PerpetualTaskClientContext clientContext =
        PerpetualTaskClientContext.builder().clientParams(clientParamMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(
        getPerpetualTaskType(), deploymentSummary.getAccountIdentifier(), clientContext, schedule, false, "");
  }

  protected final ServiceOutcome getServiceOutcomeFromAmbiance(Ambiance ambiance) {
    return (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
  }

  protected final InfrastructureOutcome getInfrastructureOutcomeFromAmbiance(Ambiance ambiance) {
    return (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));
  }

  // ---------------------------- PRIVATE METHODS ---------------------------

  private void createOrUpdateInstances(List<Instance> oldInstances, List<Instance> newInstances) {
    //    we delete the instance oldInstances - newInstances
    //    we create the instance newInstances - oldInstances
    // also update common instances with the newInstances details

    // Every instance will have a unique key
    // for k8s pods:  podInfo.getName() + podInfo.getNamespace() + getImageInStringFormat(podInfo)
    // We use this info to compare new and old instances
  }

  private U getDeploymentInfrastructureMapping(
      String accountId, String orgId, String projectId, String infrastructureMappingId) {
    Optional<InfrastructureMapping> infrastructureMappingOptional =
        infrastructureMappingRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndId(
            accountId, orgId, projectId, infrastructureMappingId);
    return validateAndReturnInfrastructureMapping(infrastructureMappingOptional.get());
  }

  // TODO need to check how to handle rollback
  private void syncInstancesInternal(U infrastructureMapping, DeploymentSummary newDeploymentSummary,
      RollbackInfo rollbackInfo, X delegateResponseData, InstanceSyncFlowType instanceSyncFlow) {
    Multimap<T, Instance> instanceHandlerKeyVsInstanceMap = ArrayListMultimap.create();
    loadInstanceHandlerKeyVsInstanceMap(infrastructureMapping, instanceHandlerKeyVsInstanceMap);

    // If its a perpetual task response, then delegate response data contains new instances info
    // Compare with corresponding instances in DB using instance handler key and process them
    if (instanceSyncFlow == InstanceSyncFlowType.PERPETUAL_TASK) {
      processInstanceSync(infrastructureMapping, instanceHandlerKeyVsInstanceMap, delegateResponseData, null);
      return;
    }

    if (instanceSyncFlow == InstanceSyncFlowType.NEW_DEPLOYMENT) {
      // Do instance sync for only for key corresponding to new deployment summary
      T instanceHandlerKey = getInstanceHandlerKey(validateAndReturnDeploymentInfo(newDeploymentSummary));
      processInstanceSyncForGivenInstanceHandlerKey(
          infrastructureMapping, instanceHandlerKey, instanceHandlerKeyVsInstanceMap, newDeploymentSummary);
      return;
    }

    for (T instanceHandlerKey : instanceHandlerKeyVsInstanceMap.keySet()) {
      // In case of Manual Sync, do instance sync for all keys corresponding to instances in DB
      processInstanceSyncForGivenInstanceHandlerKey(
          infrastructureMapping, instanceHandlerKey, instanceHandlerKeyVsInstanceMap, null);
    }
  }

  private void processInstanceSyncForGivenInstanceHandlerKey(U infrastructureMapping, T instanceHandlerKey,
      Multimap<T, Instance> instanceHandlerKeyVsInstanceMap, DeploymentSummary deploymentSummary) {
    X delegateSyncTaskResponse =
        executeDelegateSyncTaskToFetchServerInstances(infrastructureMapping, instanceHandlerKey);
    processInstanceSync(
        infrastructureMapping, instanceHandlerKeyVsInstanceMap, delegateSyncTaskResponse, deploymentSummary);
  }

  private void processInstanceSync(U infrastructureMapping, Multimap<T, Instance> instanceHandlerKeyVsInstanceMap,
      X delegateResponseData, DeploymentSummary deploymentSummary) {
    T instanceHandlerKeyFromDelegateResponse = getInstanceHandlerKey(delegateResponseData);
    List<Instance> instancesInDB =
        new ArrayList<>(instanceHandlerKeyVsInstanceMap.get(instanceHandlerKeyFromDelegateResponse));
    List<Z> serverInstances = getServerInstancesFromDelegateResponse(delegateResponseData);
    if (deploymentSummary == null) {
      // In case of perpetual task, deployment summary would be null
      // required to be constructed from existing instances
      deploymentSummary = instancesInDB.size() > 0 ? generateDeploymentSummaryFromInstance(instancesInDB.get(0)) : null;
    }
    List<Instance> instancesFromServer =
        getInstancesFromServerInstances(infrastructureMapping, serverInstances, deploymentSummary);
    createOrUpdateInstances(instancesInDB, instancesFromServer);
  }

  private Instance buildInstance(U infrastructureMapping, Z serverInstance, DeploymentSummary deploymentSummary) {
    // TODO build instance base
    Instance.InstanceBuilder instanceBuilder = Instance.builder();
    instanceBuilder.instanceInfo(getInstanceInfo(serverInstance));
    // Why don't we have common field for instane info in mongo
    // rather having separate field names for different deployment type
    // TODO set instance info

    buildInstanceCustom(instanceBuilder, serverInstance);

    return instanceBuilder.build();
  }

  private List<Instance> getInstancesFromServerInstances(
      U infrastructureMapping, List<Z> serverInstances, DeploymentSummary deploymentSummary) {
    List<Instance> instances = new ArrayList<>();
    for (Z serverInstance : serverInstances) {
      instances.add(buildInstance(infrastructureMapping, serverInstance, deploymentSummary));
    }
    return instances;
  }

  private DeploymentSummary generateDeploymentSummaryFromInstance(Instance instance) {
    if (instance == null) {
      return null;
    }
    return DeploymentSummary.builder()
        .accountIdentifier(instance.getAccountIdentifier())
        .orgIdentifier(instance.getOrgIdentifier())
        .projectIdentifier(instance.getProjectIdentifier())
        .infrastructureMappingId(instance.getInfrastructureMappingId())
        .pipelineExecutionName(instance.getLastPipelineExecutionName())
        .pipelineExecutionId(instance.getLastPipelineExecutionId())
        .deployedAt(System.currentTimeMillis())
        .deployedById(AUTO_SCALE)
        .deployedByName(AUTO_SCALE)
        .deploymentInfo(getEmptyDeploymentInfoObject())
        .build();
  }

  private void loadInstanceHandlerKeyVsInstanceMap(
      U infrastrastructureMapping, Multimap<T, Instance> instanceHandlerKeyVsInstanceMap) {
    List<Instance> instancesInDB = getInstances(infrastrastructureMapping);

    for (Instance instance : instancesInDB) {
      Y instanceInfo = validateAndReturnInstanceInfo(instance.getInstanceInfo());
      // TODO Check with Anshul if this interpretation is correct
      T instanceHandlerKey = getInstanceHandlerKey(instanceInfo);
      instanceHandlerKeyVsInstanceMap.put(instanceHandlerKey, instance);
    }
  }

  private List<Instance> getInstances(InfrastructureMapping infrastructureMapping) {
    return instanceRepository.getInstances(infrastructureMapping.getAccountIdentifier(),
        infrastructureMapping.getOrgIdentifier(), infrastructureMapping.getProjectIdentifier(),
        infrastructureMapping.getId());
  }

  private Map<String, String> prepareClientParamMapForPerpetualTask(DeploymentSummary deploymentSummary) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(InstanceSyncConstants.HARNESS_ACCOUNT_IDENTIFIER, deploymentSummary.getAccountIdentifier());
    clientParamMap.put(InstanceSyncConstants.HARNESS_ORG_IDENTIFIER, deploymentSummary.getOrgIdentifier());
    clientParamMap.put(InstanceSyncConstants.HARNESS_PROJECT_IDENTIFIER, deploymentSummary.getProjectIdentifier());
    clientParamMap.put(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, deploymentSummary.getInfrastructureMappingId());
    return clientParamMap;
  }
}