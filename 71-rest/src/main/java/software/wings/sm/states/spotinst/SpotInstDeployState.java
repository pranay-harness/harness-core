package software.wings.sm.states.spotinst;

import static com.google.common.collect.Maps.newHashMap;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstDeployTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.spotinst.model.SpotInstConstants;
import lombok.Getter;
import lombok.Setter;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.SpotInstInfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.AwsStateHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstDeployState extends State {
  @Inject private transient AppService appService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient SpotInstStateHelper spotInstStateHelper;
  @Inject private transient AwsStateHelper awsStateHelper;

  @Getter @Setter private Integer instanceCount;
  @Getter @Setter private InstanceUnitType instanceUnitType = InstanceUnitType.PERCENTAGE;
  @Getter @Setter private Integer downsizeInstanceCount;
  @Getter @Setter private InstanceUnitType downsizeInstanceUnitType = InstanceUnitType.PERCENTAGE;
  public static final String SPOTINST_DEPLOY_COMMAND = "SpotInst Deploy";

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public SpotInstDeployState(String name) {
    super(name, StateType.SPOTINST_DEPLOY.name());
  }

  public SpotInstDeployState(String name, String stateType) {
    super(name, stateType);
  }

  public boolean isRollback() {
    return false;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, SpotInstConstants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();

    SpotInstInfrastructureMapping spotInstInfrastructureMapping =
        (SpotInstInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    // fetch setupContextElement
    SpotInstSetupContextElement spotInstSetupContextElement =
        context.<SpotInstSetupContextElement>getContextElementList(ContextElementType.SPOTINST_SERVICE_SETUP)
            .stream()
            .filter(cse -> phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(SpotInstSetupContextElement.builder().build());

    // create activity
    Activity activity = spotInstStateHelper.createActivity(context, null, getStateType(), SPOTINST_DEPLOY_COMMAND);

    // Details for SpotInstConfig
    SettingAttribute settingAttribute = settingsService.get(spotInstInfrastructureMapping.getSpotinstConnectorId());
    SpotInstConfig spotInstConfig = (SpotInstConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> spotinstEncryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) spotInstConfig, context.getAppId(), context.getWorkflowExecutionId());

    // Details for AwsConfig
    settingAttribute = settingsService.get(spotInstInfrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> awsEncryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    // Calculate upsize and downsize counts
    Integer upsizeUpdateCount = getUpsizeUpdateCount(spotInstSetupContextElement);
    Integer downsizeUpdateCount = getDownsizeUpdateCount(upsizeUpdateCount, spotInstSetupContextElement);

    // Generate DeployStateExecutionData
    SpotInstDeployStateExecutionData stateExecutionData =
        geDeployStateExecutionData(spotInstSetupContextElement, activity, upsizeUpdateCount, downsizeUpdateCount);

    // Generate CommandRequest to be sent to delegate

    SpotInstTaskParameters spotInstTaskParameters = getDeployTaskParameters(context, app, activity.getUuid(),
        upsizeUpdateCount, downsizeUpdateCount, spotInstInfrastructureMapping, spotInstSetupContextElement);

    SpotInstCommandRequest request = SpotInstCommandRequest.builder()
                                         .awsConfig(awsConfig)
                                         .spotInstConfig(spotInstConfig)
                                         .awsEncryptionDetails(awsEncryptedDataDetails)
                                         .spotinstEncryptionDetails(spotinstEncryptedDataDetails)
                                         .spotInstTaskParameters(spotInstTaskParameters)
                                         .build();

    stateExecutionData.setSpotinstCommandRequest(request);
    DelegateTask task =
        spotInstStateHelper.getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.SPOTINST_COMMAND_TASK,
            activity.getUuid(), env.getUuid(), spotInstInfrastructureMapping.getUuid(), new Object[] {request},
            spotInstStateHelper.generateTimeOutForDelegateTask(spotInstTaskParameters.getTimeoutIntervalInMin()));

    delegateService.queueTask(task);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    SpotInstTaskExecutionResponse executionResponse =
        (SpotInstTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    SpotInstDeployTaskResponse spotInstDeployTaskResponse =
        (SpotInstDeployTaskResponse) executionResponse.getSpotInstTaskResponse();

    SpotInstDeployStateExecutionData stateExecutionData =
        (SpotInstDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    SpotInstInfrastructureMapping spotInstInfrastructureMapping =
        (SpotInstInfrastructureMapping) infrastructureMappingService.get(
            stateExecutionData.getAppId(), stateExecutionData.getInfraId());

    InstanceElementListParam instanceElementListParam =
        InstanceElementListParamBuilder.anInstanceElementListParam()
            .withInstanceElements(awsStateHelper.generateInstanceElements(
                spotInstDeployTaskResponse.getEc2InstancesAdded(), spotInstInfrastructureMapping, context))
            .build();

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  protected SpotInstTaskParameters getDeployTaskParameters(ExecutionContext context, Application app, String activityId,
      Integer upsizeUpdateCount, Integer downsizeUpdateCount,
      SpotInstInfrastructureMapping spotInstInfrastructureMapping,
      SpotInstSetupContextElement spotInstSetupContextElement) {
    ElastiGroup newElastiGroup = spotInstSetupContextElement.getNewElastiGroupOriginalConfig().clone();
    ElastiGroupCapacity newElastiGroupCapacity = newElastiGroup.getCapacity();

    newElastiGroupCapacity.setTarget(upsizeUpdateCount);
    boolean isFinalDeployState = isFinalDeployState(spotInstSetupContextElement);
    if (!isFinalDeployState) {
      newElastiGroupCapacity.setMinimum(upsizeUpdateCount);
      newElastiGroupCapacity.setMaximum(upsizeUpdateCount);
    }

    ElastiGroup oldElastiGroup = spotInstSetupContextElement.getOldElastiGroupOriginalConfig() != null
        ? spotInstSetupContextElement.getOldElastiGroupOriginalConfig().clone()
        : null;
    if (oldElastiGroup != null) {
      ElastiGroupCapacity oldElastiGroupCapacity = oldElastiGroup.getCapacity();
      oldElastiGroupCapacity.setTarget(upsizeUpdateCount);
      oldElastiGroupCapacity.setMaximum(upsizeUpdateCount);
      oldElastiGroupCapacity.setMinimum(upsizeUpdateCount);
    }

    SpotInstCommandRequest commandRequest = spotInstSetupContextElement.getCommandRequest();
    return SpotInstDeployTaskParameters.builder()
        .accountId(app.getAccountId())
        .appId(app.getAppId())
        .activityId(activityId)
        .awsRegion(spotInstInfrastructureMapping.getAwsRegion())
        .commandName(SPOTINST_DEPLOY_COMMAND)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .newElastiGroupWithUpdatedCapacity(newElastiGroup)
        .oldElastiGroupWithUpdatedCapacity(oldElastiGroup)
        .resizeNewFirst(ResizeStrategy.RESIZE_NEW_FIRST.equals(spotInstSetupContextElement.getResizeStrategy()))
        .timeoutIntervalInMin(commandRequest.getSpotInstTaskParameters().getTimeoutIntervalInMin())
        .build();
  }

  private boolean isFinalDeployState(SpotInstSetupContextElement spotInstSetupContextElement) {
    if (PERCENTAGE == instanceUnitType && instanceCount == 100) {
      return true;
    }

    if (PERCENTAGE != instanceUnitType && getMaxInstanceCountToBeUsed(spotInstSetupContextElement) <= instanceCount) {
      return true;
    }

    return false;
  }

  private SpotInstDeployStateExecutionData geDeployStateExecutionData(SpotInstSetupContextElement setupContextElement,
      Activity activity, Integer upsizeUpdateCount, Integer downsizeUpdateCount) {
    ElastiGroup elastiGroup = setupContextElement.getSpotInstSetupTaskResponse().getNewElastiGroup();

    return SpotInstDeployStateExecutionData.builder()
        .activityId(activity.getUuid())
        .commandName(SPOTINST_DEPLOY_COMMAND)
        .elastiGroupName(elastiGroup.getName())
        .elastiGroupId(elastiGroup.getId())
        .desiredCount(upsizeUpdateCount)
        .appId(setupContextElement.getAppId())
        .envId(setupContextElement.getEnvId())
        .infraId(setupContextElement.getInfraMappingId())
        .serviceId(setupContextElement.getServiceId())
        .downsizeCount(downsizeUpdateCount)
        .newElastiGroupOriginalConfig(setupContextElement.getNewElastiGroupOriginalConfig())
        .oldElastiGroupOriginalConfig(setupContextElement.getOldElastiGroupOriginalConfig())
        .build();
  }

  protected Integer getUpsizeUpdateCount(SpotInstSetupContextElement setupContextElement) {
    Integer count = getMaxInstanceCountToBeUsed(setupContextElement);
    return getInstanceCountToUpdate(count, instanceCount, instanceUnitType, true);
  }

  private Integer getMaxInstanceCountToBeUsed(SpotInstSetupContextElement setupContextElement) {
    return setupContextElement.isUseCurrentRunningInstanceCount() ? setupContextElement.getCurrentRunningInstanceCount()
                                                                  : setupContextElement.getMaxInstanceCount();
  }

  @VisibleForTesting
  protected Integer getDownsizeUpdateCount(Integer updateCount, SpotInstSetupContextElement setupContextElement) {
    // if downsizeInstanceCount is not set, use same updateCount as upsize
    Integer downsizeUpdationCount = updateCount;

    Integer instanceCount = getMaxInstanceCountToBeUsed(setupContextElement);
    if (downsizeInstanceCount != null) {
      downsizeUpdationCount =
          getInstanceCountToUpdate(instanceCount, downsizeInstanceCount, downsizeInstanceUnitType, false);
    }
    return downsizeUpdationCount;
  }

  private Integer getInstanceCountToUpdate(
      Integer maxInstanceCount, Integer instanceCountVal, InstanceUnitType unitType, boolean upsize) {
    Integer updateCount;
    if (PERCENTAGE == unitType) {
      int percent = Math.min(instanceCountVal, 100);
      int count = (int) Math.round((percent * maxInstanceCount) / 100.0);
      if (upsize) {
        // if use inputs 30%, means count after this phase deployment should be 30% of maxInstances
        updateCount = Math.max(count, 1);
      } else {
        updateCount = Math.max(count, 0);
        updateCount = maxInstanceCount - updateCount;
      }
    } else {
      if (upsize) {
        updateCount = instanceCountVal;
      } else {
        updateCount = maxInstanceCount - instanceCountVal;
      }
    }
    return updateCount;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = newHashMap();
    if (instanceCount == null || instanceCount < 0) {
      invalidFields.put("instanceCount", "Instance count needs to be populated");
    }
    return invalidFields;
  }
}
