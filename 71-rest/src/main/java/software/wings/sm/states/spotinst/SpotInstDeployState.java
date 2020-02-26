package software.wings.sm.states.spotinst;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.spotinst.model.SpotInstConstants.DELETE_NEW_ELASTI_GROUP;
import static io.harness.spotinst.model.SpotInstConstants.DEPLOYMENT_ERROR;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
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
import lombok.Getter;
import lombok.Setter;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.SpotinstDummyCommandUnit;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest.SpotInstCommandRequestBuilder;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
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
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject private transient SpotInstStateHelper spotInstStateHelper;
  @Inject private transient AwsStateHelper awsStateHelper;

  @Getter @Setter private Integer instanceCount;
  @Getter @Setter private InstanceUnitType instanceUnitType = InstanceUnitType.PERCENTAGE;
  @Getter @Setter private Integer downsizeInstanceCount;
  @Getter @Setter private InstanceUnitType downsizeInstanceUnitType = InstanceUnitType.PERCENTAGE;
  public static final String SPOTINST_DEPLOY_COMMAND = "SpotInst Deploy";

  public SpotInstDeployState(String name) {
    super(name, StateType.SPOTINST_DEPLOY.name());
  }

  public SpotInstDeployState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
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

  protected boolean allPhaseRollbackDone(ExecutionContext context) {
    return false;
  }

  protected void markAllPhaseRollbackDone(ExecutionContext context) {
    /**
     * No implementation needed for Spotinst Deploy step.
     * Only needed in the rollback step
     */
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    if (isRollback() && allPhaseRollbackDone(context)) {
      return ExecutionResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build();
    }
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();

    AwsAmiInfrastructureMapping awsAmiInfrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    // fetch setupContextElement
    SpotInstSetupContextElement spotInstSetupContextElement =
        context.<SpotInstSetupContextElement>getContextElementList(ContextElementType.SPOTINST_SERVICE_SETUP)
            .stream()
            .filter(cse -> context.fetchInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(SpotInstSetupContextElement.builder().build());

    // create activity
    List<CommandUnit> commandUnitList = null;
    if (spotInstSetupContextElement.isBlueGreen()) {
      commandUnitList = ImmutableList.of(new SpotinstDummyCommandUnit(UP_SCALE_COMMAND_UNIT),
          new SpotinstDummyCommandUnit(UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT),
          new SpotinstDummyCommandUnit(DEPLOYMENT_ERROR));
    } else {
      commandUnitList = newArrayList();
      if (isRollback() || ResizeStrategy.RESIZE_NEW_FIRST == spotInstSetupContextElement.getResizeStrategy()) {
        commandUnitList.add(new SpotinstDummyCommandUnit(UP_SCALE_COMMAND_UNIT));
        commandUnitList.add(new SpotinstDummyCommandUnit(UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
        commandUnitList.add(new SpotinstDummyCommandUnit(DOWN_SCALE_COMMAND_UNIT));
        commandUnitList.add(new SpotinstDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
      } else {
        commandUnitList.add(new SpotinstDummyCommandUnit(DOWN_SCALE_COMMAND_UNIT));
        commandUnitList.add(new SpotinstDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
        commandUnitList.add(new SpotinstDummyCommandUnit(UP_SCALE_COMMAND_UNIT));
        commandUnitList.add(new SpotinstDummyCommandUnit(UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
      }
      if (isRollback()) {
        commandUnitList.add(new SpotinstDummyCommandUnit(DELETE_NEW_ELASTI_GROUP));
      }
      commandUnitList.add(new SpotinstDummyCommandUnit(DEPLOYMENT_ERROR));
    }

    Activity activity = spotInstStateHelper.createActivity(
        context, null, getStateType(), SPOTINST_DEPLOY_COMMAND, CommandUnitType.SPOTINST_DEPLOY, commandUnitList);

    // Generate DeployStateExeuctionData, contains commandRequest object.
    SpotInstDeployStateExecutionData stateExecutionData =
        generateStateExecutionData(spotInstSetupContextElement, activity, awsAmiInfrastructureMapping, context, app);

    SpotInstCommandRequest spotInstCommandRequest = stateExecutionData.getSpotinstCommandRequest();

    DelegateTask task =
        spotInstStateHelper.getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.SPOTINST_COMMAND_TASK,
            activity.getUuid(), env.getUuid(), awsAmiInfrastructureMapping.getUuid(), spotInstCommandRequest);

    delegateService.queueTask(task);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(activity.getUuid()))
        .stateExecutionData(stateExecutionData)
        .async(true)
        .build();
  }

  protected SpotInstDeployStateExecutionData generateStateExecutionData(
      SpotInstSetupContextElement spotInstSetupContextElement, Activity activity,
      AwsAmiInfrastructureMapping awsAmiInfrastructureMapping, ExecutionContext context, Application app) {
    // Calculate upsize and downsize counts
    Integer upsizeUpdateCount = getUpsizeUpdateCount(spotInstSetupContextElement);
    Integer downsizeUpdateCount = getDownsizeUpdateCount(upsizeUpdateCount, spotInstSetupContextElement);

    // Generate DeployStateExecutionData
    SpotInstDeployStateExecutionData stateExecutionData =
        geDeployStateExecutionData(spotInstSetupContextElement, activity, upsizeUpdateCount, downsizeUpdateCount);

    // Generate CommandRequest to be sent to delegate
    SpotInstCommandRequestBuilder requestBuilder =
        spotInstStateHelper.generateSpotInstCommandRequest(awsAmiInfrastructureMapping, context);
    SpotInstTaskParameters spotInstTaskParameters = getDeployTaskParameters(context, app, activity.getUuid(),
        upsizeUpdateCount, downsizeUpdateCount, awsAmiInfrastructureMapping, spotInstSetupContextElement);

    SpotInstCommandRequest request = requestBuilder.spotInstTaskParameters(spotInstTaskParameters).build();
    stateExecutionData.setSpotinstCommandRequest(request);
    return stateExecutionData;
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    SpotInstTaskExecutionResponse executionResponse =
        (SpotInstTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    if (isRollback() && ExecutionStatus.SUCCESS == executionStatus) {
      markAllPhaseRollbackDone(context);
    }

    SpotInstDeployTaskResponse spotInstDeployTaskResponse =
        (SpotInstDeployTaskResponse) executionResponse.getSpotInstTaskResponse();

    SpotInstDeployStateExecutionData stateExecutionData =
        (SpotInstDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    AwsAmiInfrastructureMapping awsAmiInfrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
            stateExecutionData.getAppId(), stateExecutionData.getInfraId());

    List<InstanceElement> instanceElements = newArrayList();
    if (spotInstDeployTaskResponse != null) {
      List<InstanceElement> newInstanceElements = awsStateHelper.generateInstanceElements(
          spotInstDeployTaskResponse.getEc2InstancesAdded(), awsAmiInfrastructureMapping, context);
      if (isNotEmpty(newInstanceElements)) {
        // These are newly launched instances, set NewInstance = true for verification service
        newInstanceElements.forEach(instanceElement -> instanceElement.setNewInstance(true));
        instanceElements.addAll(newInstanceElements);
      }

      List<InstanceElement> existingInstanceElements = awsStateHelper.generateInstanceElements(
          spotInstDeployTaskResponse.getEc2InstancesExisting(), awsAmiInfrastructureMapping, context);
      if (isNotEmpty(existingInstanceElements)) {
        // These are Existing instances for older elastiGroup, set NewInstance = false for verification service
        existingInstanceElements.forEach(instanceElement -> instanceElement.setNewInstance(false));
        instanceElements.addAll(existingInstanceElements);
      }
    }
    InstanceElementListParam instanceElementListParam =
        InstanceElementListParam.builder().instanceElements(instanceElements).build();

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .contextElement(instanceElementListParam)
        .notifyElement(instanceElementListParam)
        .build();
  }

  protected SpotInstTaskParameters getDeployTaskParameters(ExecutionContext context, Application app, String activityId,
      Integer upsizeUpdateCount, Integer downsizeUpdateCount, AwsAmiInfrastructureMapping awsAmiInfrastructureMapping,
      SpotInstSetupContextElement spotInstSetupContextElement) {
    ElastiGroup newElastiGroup = spotInstSetupContextElement.getNewElastiGroupOriginalConfig().clone();
    ElastiGroupCapacity newElastiGroupCapacity = newElastiGroup.getCapacity();

    newElastiGroupCapacity.setTarget(upsizeUpdateCount);
    boolean isFinalDeployState = isFinalDeployState(spotInstSetupContextElement);
    if (!isFinalDeployState) {
      newElastiGroupCapacity.setMinimum(upsizeUpdateCount);
      newElastiGroupCapacity.setMaximum(upsizeUpdateCount);
    } else {
      newElastiGroupCapacity.setMinimum(
          spotInstSetupContextElement.getNewElastiGroupOriginalConfig().getCapacity().getMinimum());
      newElastiGroupCapacity.setMaximum(
          spotInstSetupContextElement.getNewElastiGroupOriginalConfig().getCapacity().getMaximum());
    }

    ElastiGroup oldElastiGroup = spotInstSetupContextElement.getOldElastiGroupOriginalConfig() != null
        ? spotInstSetupContextElement.getOldElastiGroupOriginalConfig().clone()
        : null;
    if (oldElastiGroup != null) {
      ElastiGroupCapacity oldElastiGroupCapacity = oldElastiGroup.getCapacity();
      if (isFinalDeployState) {
        oldElastiGroupCapacity.setTarget(0);
        oldElastiGroupCapacity.setMaximum(0);
        oldElastiGroupCapacity.setMinimum(0);
      } else {
        oldElastiGroupCapacity.setTarget(downsizeUpdateCount);
        oldElastiGroupCapacity.setMaximum(downsizeUpdateCount);
        oldElastiGroupCapacity.setMinimum(downsizeUpdateCount);
      }
    }

    return generateSpotInstDeployTaskParameters(app, activityId, awsAmiInfrastructureMapping, context,
        spotInstSetupContextElement, oldElastiGroup, newElastiGroup);
  }

  protected SpotInstDeployTaskParameters generateSpotInstDeployTaskParameters(Application app, String activityId,
      AwsAmiInfrastructureMapping awsAmiInfrastructureMapping, ExecutionContext context,
      SpotInstSetupContextElement spotInstSetupContextElement, ElastiGroup oldElastiGroup, ElastiGroup newElastiGroup) {
    SpotInstCommandRequest commandRequest = spotInstSetupContextElement.getCommandRequest();

    boolean isBlueGreen = spotInstSetupContextElement.isBlueGreen();
    return SpotInstDeployTaskParameters.builder()
        .accountId(app.getAccountId())
        .appId(app.getAppId())
        .activityId(activityId)
        .awsRegion(awsAmiInfrastructureMapping.getRegion())
        .commandName(SPOTINST_DEPLOY_COMMAND)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .rollback(isRollback())
        .resizeNewFirst(ResizeStrategy.RESIZE_NEW_FIRST == spotInstSetupContextElement.getResizeStrategy())
        .blueGreen(isBlueGreen)
        .timeoutIntervalInMin(commandRequest.getSpotInstTaskParameters().getTimeoutIntervalInMin())
        .oldElastiGroupWithUpdatedCapacity(oldElastiGroup)
        .newElastiGroupWithUpdatedCapacity(newElastiGroup)
        .build();
  }

  private boolean isFinalDeployState(SpotInstSetupContextElement spotInstSetupContextElement) {
    if (PERCENTAGE == instanceUnitType && instanceCount == 100) {
      return true;
    }

    if (PERCENTAGE != instanceUnitType
        && getTargetInstanceCountToBeUsed(spotInstSetupContextElement) <= instanceCount) {
      return true;
    }

    return false;
  }

  protected SpotInstDeployStateExecutionData geDeployStateExecutionData(SpotInstSetupContextElement setupContextElement,
      Activity activity, Integer newGroupUpdateCount, Integer oldGroupUpdateCount) {
    ElastiGroup newElastiGroup = setupContextElement.getNewElastiGroupOriginalConfig();
    ElastiGroup emptyElastiGroup = ElastiGroup.builder().build();
    newElastiGroup = newElastiGroup == null ? emptyElastiGroup : newElastiGroup;
    ElastiGroup oldElastiGroup = setupContextElement.getOldElastiGroupOriginalConfig();
    oldElastiGroup = oldElastiGroup == null ? emptyElastiGroup : oldElastiGroup;

    return SpotInstDeployStateExecutionData.builder()
        .activityId(activity.getUuid())
        .commandName(SPOTINST_DEPLOY_COMMAND)
        .newElastiGroupName(newElastiGroup.getName())
        .newElastiGroupId(newElastiGroup.getId())
        .newDesiredCount(newGroupUpdateCount)
        .oldElastiGroupName(oldElastiGroup.getName())
        .oldElastiGroupId(oldElastiGroup.getId())
        .oldDesiredCount(oldGroupUpdateCount)
        .infraId(setupContextElement.getInfraMappingId())
        .serviceId(setupContextElement.getServiceId())
        .appId(setupContextElement.getAppId())
        .envId(setupContextElement.getEnvId())
        .newElastiGroupOriginalConfig(setupContextElement.getNewElastiGroupOriginalConfig())
        .oldElastiGroupOriginalConfig(setupContextElement.getOldElastiGroupOriginalConfig())
        .build();
  }

  protected Integer getUpsizeUpdateCount(SpotInstSetupContextElement setupContextElement) {
    Integer count = getTargetInstanceCountToBeUsed(setupContextElement);
    return getInstanceCountToUpdate(count, instanceCount, instanceUnitType);
  }

  private Integer getTargetInstanceCountToBeUsed(SpotInstSetupContextElement setupContextElement) {
    return setupContextElement.getNewElastiGroupOriginalConfig().getCapacity().getTarget();
  }

  @VisibleForTesting
  protected Integer getDownsizeUpdateCount(Integer updateCount, SpotInstSetupContextElement setupContextElement) {
    ElastiGroup oldElastigroup = setupContextElement.getOldElastiGroupOriginalConfig();
    if (oldElastigroup == null) {
      return null;
    }
    int oldElastigroupTargetAtStart = oldElastigroup.getCapacity().getTarget();
    if (downsizeInstanceCount == null) {
      return Math.max(0, oldElastigroupTargetAtStart - updateCount);
    } else {
      if (InstanceUnitType.COUNT == downsizeInstanceUnitType) {
        return downsizeInstanceCount;
      } else {
        int percent = Math.min(downsizeInstanceCount, 100);
        return (int) ((percent * oldElastigroupTargetAtStart) / 100.0);
      }
    }
  }

  private Integer getInstanceCountToUpdate(
      Integer maxInstanceCount, Integer instanceCountVal, InstanceUnitType unitType) {
    int updateCount;
    if (PERCENTAGE == unitType) {
      int percent = Math.min(instanceCountVal, 100);
      int count = (int) Math.round((percent * maxInstanceCount) / 100.0);
      // if use inputs 30%, means count after this phase deployment should be 30% of maxInstances
      updateCount = Math.max(count, 1);
    } else {
      updateCount = Math.min(instanceCountVal, maxInstanceCount);
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
    if (!isRollback() && (instanceCount == null || instanceCount < 0)) {
      invalidFields.put("instanceCount", "Instance count needs to be populated");
    }
    return invalidFields;
  }
}
