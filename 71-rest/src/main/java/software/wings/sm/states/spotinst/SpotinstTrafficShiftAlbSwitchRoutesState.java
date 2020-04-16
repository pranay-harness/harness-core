package software.wings.sm.states.spotinst;

import static io.harness.context.ContextElementType.SPOTINST_SERVICE_SETUP;
import static io.harness.spotinst.model.SpotInstConstants.DEPLOYMENT_ERROR;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.PHASE_PARAM;
import static io.harness.spotinst.model.SpotInstConstants.SWAP_ROUTES_COMMAND_UNIT;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Collections.singletonList;
import static software.wings.sm.states.spotinst.SpotInstListenerUpdateState.SPOTINST_LISTENER_UPDATE_COMMAND;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSwapRoutesParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.SpotinstDummyCommandUnit;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

@ToString
@Slf4j
public class SpotinstTrafficShiftAlbSwitchRoutesState extends State {
  @Getter @Setter private boolean downsizeOldElastigroup;
  @Getter @Setter private String newElastigroupWeightExpr;

  @Inject private DelegateService delegateService;
  @Inject private ActivityService activityService;
  @Inject private SpotInstStateHelper spotinstStateHelper;

  public SpotinstTrafficShiftAlbSwitchRoutesState(String name) {
    super(name, StateType.SPOTINST_LISTENER_ALB_SHIFT.name());
  }

  SpotinstTrafficShiftAlbSwitchRoutesState(String name, String stateType) {
    super(name, stateType);
  }

  @VisibleForTesting
  SpotinstTrafficShiftAlbSwitchRoutesState() {
    super("stateName", StateType.SPOTINST_LISTENER_ALB_SHIFT.name());
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, boolean rollback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    notNullCheck("Phase element is null", phaseElement);
    ServiceElement serviceElement = phaseElement.getServiceElement();

    ContextElement contextElement = context.getContextElement(SPOTINST_SERVICE_SETUP);
    if (!(contextElement instanceof SpotinstTrafficShiftAlbSetupElement)) {
      throw new InvalidRequestException("Did not find Setup element of class SpotinstTrafficShiftAlbSetupElement");
    }
    SpotinstTrafficShiftAlbSetupElement setupElement = (SpotinstTrafficShiftAlbSetupElement) contextElement;
    SpotinstTrafficShiftDataBag dataBag = spotinstStateHelper.getDataBag(context);

    Activity activity =
        spotinstStateHelper.createActivity(context, null, getStateType(), SPOTINST_LISTENER_UPDATE_COMMAND,
            CommandUnitDetails.CommandUnitType.SPOTINST_UPDATE_LISTENER, getCommandUnits());

    SpotinstTrafficShiftAlbSwapRoutesExecutionData data =
        SpotinstTrafficShiftAlbSwapRoutesExecutionData.builder()
            .activityId(activity.getUuid())
            .serviceId(serviceElement.getUuid())
            .envId(dataBag.getEnv().getUuid())
            .appId(dataBag.getApp().getUuid())
            .infraMappingId(dataBag.getInfrastructureMapping().getUuid())
            .commandName(SPOTINST_LISTENER_UPDATE_COMMAND)
            .newElastigroupOriginalConfig(setupElement.getNewElastiGroupOriginalConfig())
            .oldElastigroupOriginalConfig(setupElement.getOldElastiGroupOriginalConfig())
            .build();

    SpotinstTrafficShiftAlbSwapRoutesParameters parameters =
        SpotinstTrafficShiftAlbSwapRoutesParameters.builder()
            .appId(dataBag.getApp().getUuid())
            .accountId(dataBag.getApp().getAccountId())
            .activityId(activity.getUuid())
            .commandName(SPOTINST_LISTENER_UPDATE_COMMAND)
            .workflowExecutionId(context.getWorkflowExecutionId())
            .timeoutIntervalInMin(setupElement.getTimeoutIntervalInMin())
            .awsRegion(dataBag.getInfrastructureMapping().getRegion())
            .newElastigroup(setupElement.getNewElastiGroupOriginalConfig())
            .oldElastigroup(setupElement.getOldElastiGroupOriginalConfig())
            .elastigroupNamePrefix(setupElement.getElastigroupNamePrefix())
            .downsizeOldElastigroup(downsizeOldElastigroup)
            .rollback(rollback)
            .details(setupElement.getDetailsWithTargetGroups())
            .newElastigroupWeight(getNewElastigroupWeight(context))
            .build();

    SpotInstCommandRequest commandRequest = SpotInstCommandRequest.builder()
                                                .spotInstTaskParameters(parameters)
                                                .awsConfig(dataBag.getAwsConfig())
                                                .spotInstConfig(dataBag.getSpotinstConfig())
                                                .awsEncryptionDetails(dataBag.getAwsEncryptedDataDetails())
                                                .spotinstEncryptionDetails(dataBag.getSpotinstEncryptedDataDetails())
                                                .build();

    DelegateTask delegateTask = spotinstStateHelper.getDelegateTask(dataBag.getApp().getAccountId(),
        dataBag.getApp().getUuid(), TaskType.SPOTINST_COMMAND_TASK, activity.getUuid(), dataBag.getEnv().getUuid(),
        dataBag.getInfrastructureMapping().getUuid(), commandRequest);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(singletonList(activity.getUuid()))
        .stateExecutionData(data)
        .async(true)
        .build();
  }

  protected int getNewElastigroupWeight(ExecutionContext context) {
    return spotinstStateHelper.renderCount(newElastigroupWeightExpr, context, 10);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected List<CommandUnit> getCommandUnits() {
    return ImmutableList.of(new SpotinstDummyCommandUnit(SWAP_ROUTES_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DOWN_SCALE_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DEPLOYMENT_ERROR));
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    SpotInstTaskExecutionResponse executionResponse =
        (SpotInstTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus() == CommandExecutionResult.CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    SpotinstTrafficShiftAlbSwapRoutesExecutionData stateExecutionData =
        (SpotinstTrafficShiftAlbSwapRoutesExecutionData) context.getStateExecutionData();
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setStatus(executionStatus);

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .stateExecutionData(stateExecutionData)
        .errorMessage(executionResponse.getErrorMessage())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context, false);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }
}