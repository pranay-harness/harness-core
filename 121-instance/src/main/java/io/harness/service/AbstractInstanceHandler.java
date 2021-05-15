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
import io.harness.entities.instance.InstanceKey;
import io.harness.entities.instanceinfo.InstanceInfo;
import io.harness.exception.GeneralException;
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
/**
 * Instance Handler Key - key used for logical orchestration of the instance handlers
 * Server Instance - represents the logical instance entity in the server
 * Instance Key - unique identifier of a server instance stored as part of instance entity for reference
 * Instance Info - details about server instance stored as part of instance entity
 */
public abstract class AbstractInstanceHandler<T extends InstanceHandlerKey, U extends InfrastructureMapping, V
                                                  extends DeploymentInfo, X extends DelegateResponseData, Y
                                                  extends InstanceInfo, Z extends ServerInstance, W extends InstanceKey>
    implements IInstanceHandler, IInstanceSyncByPerpetualTaskhandler<X>, IInstanceSyncPerpetualTaskCreator<U> {
  public static final String AUTO_SCALE = "AUTO_SCALE";

  @Inject protected InfrastructureMappingRepository infrastructureMappingRepository;
  @Inject protected OutcomeService outcomeService;
  @Inject private InstanceRepository instanceRepository;
  @Inject PerpetualTaskService perpetualTaskService;

  /**
   * The handler needs to validate if deployment info in the deployment summary is of appropriate type as per handler
   * @param deploymentSummary
   * @return
   * @throws GeneralException
   */
  protected abstract V validateAndReturnDeploymentInfo(DeploymentSummary deploymentSummary) throws GeneralException;

  /**
   * The handler needs to validate if instanceInfo is of appropriate type as per the handler
   * Its helpful to find anomalies (if any) in the instances in DB if instanceInfo doesn't match
   * @param instanceInfo
   * @return subclass of instanceInfo as per deployment type of the handler
   * @throws GeneralException
   */
  protected abstract Y validateAndReturnInstanceInfo(InstanceInfo instanceInfo) throws GeneralException;

  /**
   * The handler needs to validate if infrastructure mapping is of appropriate type as per the handler
   * It takes in the parent class object and returns concrete infrastructure mapping object as per the deployment type
   * @param infrastructureMapping
   * @return subclass of infrastructureMapping as per deployment type of the handler
   * @throws GeneralException if input infrastructure mapping is mismatch with required type
   */
  protected abstract U validateAndReturnInfrastructureMapping(InfrastructureMapping infrastructureMapping)
      throws GeneralException;

  /**
   * The handler needs to validate if the perpetual task response is of appropriate type as per the handler
   * It takes in the parent class object and returns concrete delegate response object as per the handler type
   * @param delegateResponseData instance sync perpetual task response
   * @return subclass of delegateResponseData as per deployment type of the handler
   * @throws GeneralException if delegate response data doesn't match with required response for the handler
   */
  protected abstract X validateAndReturnPerpetualTaskDelegateResponse(DelegateResponseData delegateResponseData)
      throws GeneralException;

  // TODO need to finalize definition of this method
  /**
   * The handler needs to provide a comparator method to compare if 2 instances objects are same or not
   * @param instance
   * @return
   */
  protected abstract String getInstanceUniqueIdentifier(Instance instance);

  /**
   * It returns instance handler key, used for logical handling of instances
   * Also required while creating perpetual task context map for new instance sync perpetual task
   * @param deploymentInfo
   * @return
   */
  protected abstract T getInstanceHandlerKey(V deploymentInfo);

  /**
   * Fetch instance handler key (orchestration key) using instance info from the instance entity
   * Its required to map instances in the DB to the key
   * @param instanceInfo
   * @return subtype of InstanceHandlerKey as per the handler
   */
  protected abstract T getInstanceHandlerKey(Y instanceInfo);

  /**
   * Fetch instanceHandlerKey (orchestration key) using the delegate task response
   * Its required in case of instance sync perpetual task flow to find out the corresponding instances in DB
   * for sync for the given response
   * @param delegateResponseData
   * @return
   */
  protected abstract T getInstanceHandlerKey(X delegateResponseData);

  /**
   * It is required to fetch the instance info (details related to deployment) from the server instance
   * to be stored into its corresponding instance entity in DB
   * @param serverInstance
   * @return
   */
  protected abstract Y getInstanceInfo(Z serverInstance);

  /**
   * It is required to fetch instance key from the server instance to be stored in the corresponding
   * instance entity in the DB
   * @param serverInstance
   * @return
   */
  protected abstract W getInstanceKey(Z serverInstance);

  /**
   * Fetch list of server instances (eg: k8sPodsInfo etc) from the delegate task response
   * @param delegateResponseData
   * @return
   */
  protected abstract List<Z> getServerInstancesFromDelegateResponse(X delegateResponseData);

  /**
   * Its triggers delegate sync task to fetch instances/pods details from the server
   * It is required during Manual Sync to fetch server instances and then do sync with instances in DB
   * The task fetches instances corresponding to a particular instance handler key information only
   * eg: for a given applicationName in PCF, or given autoscalingGroup in AWS etc
   * @param infrastructureMapping
   * @param instanceHandlerKey
   * @return handler specific delegate response data required for orhestration purpose
   */
  protected abstract X executeDelegateSyncTaskToFetchServerInstances(U infrastructureMapping, T instanceHandlerKey);

  // Override this method in case of specific custom instance build requirements

  /**
   * This method lets the handler to add details to the instance entity based on deployment type
   * Base instance is already created by the abstract layer, handler can add any deployment specific information
   * to the instance via this method
   * @param instanceBuilder
   * @param serverInstance
   */
  protected void buildInstanceCustom(Instance.InstanceBuilder instanceBuilder, Z serverInstance) {
    return;
  }

  /**
   * The handler needs to process the incoming pipeline deployment event and return corresponding
   * infrastructure mapping for the same
   * @param ambiance
   * @param serviceOutcome
   * @param infrastructureOutcome
   * @return
   */
  protected abstract InfrastructureMapping getInfrastructureMappingByType(
      Ambiance ambiance, ServiceOutcome serviceOutcome, InfrastructureOutcome infrastructureOutcome);

  /**
   * The handler needs to add specific deployment type information to the client context of the perpetual task
   * @param clientParamMap
   * @param instanceHandlerKey
   */
  protected abstract void populateClientParamMapForPerpetualTask(
      Map<String, String> clientParamMap, T instanceHandlerKey);

  /**
   * Perpetual task type required for the deployment type while creating instance sync perpetual task
   * @return
   */
  protected abstract String getPerpetualTaskType();

  // TODO check if this method would be require or not
  /**
   * Its required during preparing deployment summary while handling perpetual task
   * @return
   */
  protected abstract V getEmptyDeploymentInfoObject();

  // --------------------------------- Public interface Methods ---------------------------------

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

    // TODO check if required
    validateAndReturnDeploymentInfo(deploymentSummary);

    // Infrastructure mapping is already present in deployment summary
    // TODO check if required
    U infrastructureMapping = validateAndReturnInfrastructureMapping(deploymentSummary.getInfrastructureMapping());

    syncInstancesInternal(
        infrastructureMapping, deploymentSummary, rollbackInfo, null, InstanceSyncFlowType.NEW_DEPLOYMENT);
  }

  // Handle the response from instance sync perpetual task
  public final void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData delegateResponseData) {
    // Validate that incoming infrastructure mapping is of correct type or not as per the handler
    // Get infrastructure mapping subtype for orchestration purpose
    U infrastructureMappingDetails = validateAndReturnInfrastructureMapping(infrastructureMapping);

    // Get delegate response subtype for orchestration purpose
    X delegateResponse = validateAndReturnPerpetualTaskDelegateResponse(delegateResponseData);

    syncInstancesInternal(infrastructureMappingDetails, null, RollbackInfo.builder().isRollback(false).build(),
        delegateResponse, InstanceSyncFlowType.PERPETUAL_TASK);
  }

  public final void syncInstances(String accountId, String orgId, String projectId, String infrastructureMappingId,
      InstanceSyncFlowType instanceSyncFlowType) {
    U infrastructureMapping = getDeploymentInfrastructureMapping(accountId, orgId, projectId, infrastructureMappingId);

    syncInstancesInternal(
        infrastructureMapping, null, RollbackInfo.builder().isRollback(false).build(), null, instanceSyncFlowType);
  }

  public final String createPerpetualTaskForNewDeployment(
      DeploymentSummary deploymentSummary, U infrastructureMapping) {
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
    List<Z> serverInstances = getServerInstancesFromDelegateResponse(delegateResponseData);
    T instanceHandlerKeyFromDelegateResponse = getInstanceHandlerKey(delegateResponseData);
    List<Instance> instancesInDB =
        new ArrayList<>(instanceHandlerKeyVsInstanceMap.get(instanceHandlerKeyFromDelegateResponse));

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
    instanceBuilder.instanceKey(getInstanceKey(serverInstance));

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