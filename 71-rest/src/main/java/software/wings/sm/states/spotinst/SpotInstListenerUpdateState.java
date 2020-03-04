package software.wings.sm.states.spotinst;

import static io.harness.spotinst.model.SpotInstConstants.DEPLOYMENT_ERROR;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.SWAP_ROUTES_COMMAND_UNIT;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.spotinst.model.ElastiGroup;
import lombok.Getter;
import lombok.Setter;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.Environment;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstListenerUpdateState extends State {
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ActivityService activityService;
  @Inject private transient SpotInstStateHelper spotInstStateHelper;

  @Getter @Setter private boolean downsizeOldElastiGroup;
  public static final String SPOTINST_LISTENER_UPDATE_COMMAND = "SpotInst Listener Update";

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public SpotInstListenerUpdateState(String name) {
    super(name, StateType.SPOTINST_LISTENER_UPDATE.name());
  }

  public SpotInstListenerUpdateState(String name, String stateType) {
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

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Environment env = workflowStandardParams.fetchRequiredEnv();
    Application app = appService.get(context.getAppId());
    AwsAmiInfrastructureMapping awsAmiInfrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    // retrieve SpotInstSetupContextElement
    SpotInstSetupContextElement spotInstSetupContextElement =
        context.<SpotInstSetupContextElement>getContextElementList(ContextElementType.SPOTINST_SERVICE_SETUP)
            .stream()
            .filter(cse -> context.fetchInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(SpotInstSetupContextElement.builder().build());

    // create activity with details
    Activity activity = spotInstStateHelper.createActivity(context, null, getStateType(),
        SPOTINST_LISTENER_UPDATE_COMMAND, CommandUnitType.SPOTINST_UPDATE_LISTENER, getCommandUnits());

    // Generate SpotInstListenerUpdateStateExecutionData
    SpotInstListenerUpdateStateExecutionData stateExecutionData =
        geStateExecutionData(spotInstSetupContextElement, activity);

    SpotInstTaskParameters spotInstTaskParameters =
        getTaskParameters(context, app, activity.getUuid(), awsAmiInfrastructureMapping, spotInstSetupContextElement);

    // Generate CommandRequest to be sent to delegate
    SpotInstCommandRequestBuilder requestBuilder =
        spotInstStateHelper.generateSpotInstCommandRequest(awsAmiInfrastructureMapping, context);
    SpotInstCommandRequest spotInstCommandRequest =
        requestBuilder.spotInstTaskParameters(spotInstTaskParameters).build();

    stateExecutionData.setSpotinstCommandRequest(spotInstCommandRequest);
    setElastigroupFieldsInStateExecutionData(spotInstSetupContextElement, stateExecutionData);

    DelegateTask task =
        spotInstStateHelper.getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.SPOTINST_COMMAND_TASK,
            activity.getUuid(), env.getUuid(), awsAmiInfrastructureMapping.getUuid(), spotInstCommandRequest);

    delegateService.queueTask(task);
    return ExecutionResponse.builder()
        .stateExecutionData(stateExecutionData)
        .correlationIds(Arrays.asList(activity.getUuid()))
        .async(true)
        .build();
  }

  protected List<CommandUnit> getCommandUnits() {
    return ImmutableList.of(new SpotinstDummyCommandUnit(SWAP_ROUTES_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DOWN_SCALE_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DEPLOYMENT_ERROR));
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    SpotInstTaskExecutionResponse executionResponse =
        (SpotInstTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    SpotInstListenerUpdateStateExecutionData stateExecutionData =
        (SpotInstListenerUpdateStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setStatus(executionStatus);

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .stateExecutionData(stateExecutionData)
        .errorMessage(executionResponse.getErrorMessage())
        .build();
  }

  private void setElastigroupFieldsInStateExecutionData(
      SpotInstSetupContextElement spotinstSetupContextElement, SpotInstListenerUpdateStateExecutionData data) {
    ElastiGroup oldElastiGroupOriginalConfig = spotinstSetupContextElement.getOldElastiGroupOriginalConfig();
    if (oldElastiGroupOriginalConfig != null) {
      data.setOldElastiGroupId(oldElastiGroupOriginalConfig.getId());
      data.setOldElastiGroupName(oldElastiGroupOriginalConfig.getName());
    }
    ElastiGroup newElastiGroupOriginalConfig = spotinstSetupContextElement.getNewElastiGroupOriginalConfig();
    if (newElastiGroupOriginalConfig != null) {
      data.setNewElastiGroupId(newElastiGroupOriginalConfig.getId());
      data.setNewElastiGroupName(newElastiGroupOriginalConfig.getName());
    }
  }

  protected SpotInstSwapRoutesTaskParameters getTaskParameters(ExecutionContext context, Application app,
      String activityId, AwsAmiInfrastructureMapping awsAmiInfrastructureMapping,
      SpotInstSetupContextElement setupContextElement) {
    SpotInstCommandRequest commandRequest = setupContextElement.getCommandRequest();
    return SpotInstSwapRoutesTaskParameters.builder()
        .accountId(app.getAccountId())
        .appId(app.getAppId())
        .activityId(activityId)
        .awsRegion(awsAmiInfrastructureMapping.getRegion())
        .commandName(SPOTINST_LISTENER_UPDATE_COMMAND)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .downsizeOldElastiGroup(downsizeOldElastiGroup)
        .newElastiGroup(setupContextElement.getNewElastiGroupOriginalConfig())
        .oldElastiGroup(setupContextElement.getOldElastiGroupOriginalConfig())
        .elastiGroupNamePrefix(setupContextElement.getElstiGroupNamePrefix())
        .lBdetailsForBGDeploymentList(setupContextElement.getLbDetailsForBGDeployment())
        .timeoutIntervalInMin(spotInstStateHelper.getTimeoutFromCommandRequest(commandRequest))
        .rollback(isRollback())
        .build();
  }

  private SpotInstListenerUpdateStateExecutionData geStateExecutionData(
      SpotInstSetupContextElement setupContextElement, Activity activity) {
    return SpotInstListenerUpdateStateExecutionData.builder()
        .activityId(activity.getUuid())
        .commandName(SPOTINST_LISTENER_UPDATE_COMMAND)
        .appId(setupContextElement.getAppId())
        .envId(setupContextElement.getEnvId())
        .infraId(setupContextElement.getInfraMappingId())
        .serviceId(setupContextElement.getServiceId())
        .downsizeOldElastiGroup(downsizeOldElastiGroup)
        .lbDetails(setupContextElement.getLbDetailsForBGDeployment())
        .isRollback(isRollback())
        .build();
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
}
