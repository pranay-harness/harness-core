package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static java.util.Collections.singletonList;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.TaskType.AWS_AMI_ASYNC_TASK;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_TRAFFIC_SHIFT_WEIGHT;
import static software.wings.sm.states.AwsAmiSwitchRoutesState.SWAP_AUTO_SCALING_ROUTES;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.api.AmiServiceTrafficShiftAlbSetupElement;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.api.AwsAmiTrafficShiftAlbStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.SpotinstDummyCommandUnit;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesResponse;
import software.wings.service.impl.aws.model.AwsAmiTrafficShiftAlbSwitchRouteRequest;
import software.wings.service.impl.aws.model.AwsConstants;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.LogService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.spotinst.SpotInstStateHelper;
import software.wings.utils.Misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AwsAmiTrafficShiftAlbSwitchRoutesState extends State {
  @Getter @Setter private boolean downsizeOldAsg;
  @Getter @Setter private String newAutoScalingGroupWeightExpr;

  @Inject private SpotInstStateHelper spotinstStateHelper;
  @Inject private AwsAmiServiceStateHelper awsAmiServiceHelper;
  @Inject protected DelegateService delegateService;
  @Inject private ActivityService activityService;
  @Inject private LogService logService;

  public AwsAmiTrafficShiftAlbSwitchRoutesState(String name) {
    super(name, StateType.ASG_AMI_ALB_SHIFT_SWITCH_ROUTES.name());
  }

  public AwsAmiTrafficShiftAlbSwitchRoutesState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    AwsAmiSwitchRoutesResponse executionResponse = (AwsAmiSwitchRoutesResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getExecutionStatus();
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    AwsAmiTrafficShiftAlbStateExecutionData stateExecutionData =
        (AwsAmiTrafficShiftAlbStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setStatus(executionStatus);

    spotinstStateHelper.saveInstanceInfoToSweepingOutput(context, getNewAutoscalingGroupWeight(context));

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .stateExecutionData(stateExecutionData)
        .errorMessage(executionResponse.getErrorMessage())
        .build();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    Activity activity = spotinstStateHelper.createActivity(context, null, getStateType(), getName(),
        CommandUnitDetails.CommandUnitType.AWS_AMI_SWITCH_ROUTES, getCommandUnits());

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, getLogBuilder(activity), activity.getUuid());
    try {
      AmiServiceTrafficShiftAlbSetupElement setupElement = validateAndGetSetupElement(context);
      if (setupElement == null) {
        return skipResponse();
      }
      AwsAmiTrafficShiftAlbData awsAmiTrafficShiftAlbData =
          awsAmiServiceHelper.populateAlbTrafficShiftSetupData(context);

      AwsAmiTrafficShiftAlbSwitchRouteRequest trafficShiftRequest =
          buildAmiTrafficShiftSwitchRouteRequest(context, setupElement, awsAmiTrafficShiftAlbData, activity);

      AwsAmiTrafficShiftAlbStateExecutionData executionData =
          createAndEnqueueDelegateTask(context, setupElement, awsAmiTrafficShiftAlbData, activity, trafficShiftRequest);
      return successResponse(activity, executionData);
    } catch (Exception exception) {
      return taskCreationFailureResponse(activity, executionLogCallback, exception);
    }
  }

  private AmiServiceTrafficShiftAlbSetupElement validateAndGetSetupElement(ExecutionContext context) {
    ContextElement contextElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    if (!(contextElement instanceof AmiServiceTrafficShiftAlbSetupElement)) {
      if (!isRollback()) {
        throw new InvalidRequestException("Did not find Setup element of class AmiServiceTrafficShiftAlbSetupElement");
      }
      return null;
    }
    return (AmiServiceTrafficShiftAlbSetupElement) contextElement;
  }

  private AwsAmiTrafficShiftAlbStateExecutionData createAndEnqueueDelegateTask(ExecutionContext context,
      AmiServiceTrafficShiftAlbSetupElement setupElement, AwsAmiTrafficShiftAlbData awsAmiTrafficShiftAlbData,
      Activity activity, AwsAmiTrafficShiftAlbSwitchRouteRequest trafficShiftRequest) {
    AwsAmiTrafficShiftAlbStateExecutionData executionData =
        AwsAmiTrafficShiftAlbStateExecutionData.builder()
            .activityId(activity.getUuid())
            .oldAutoScalingGroupName(setupElement.getOldAutoScalingGroupName())
            .newAutoScalingGroupName(setupElement.getNewAutoScalingGroupName())
            .newAutoscalingGroupWeight(getNewAutoscalingGroupWeight(context))
            .build();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(awsAmiTrafficShiftAlbData.getApp().getAccountId())
            .appId(awsAmiTrafficShiftAlbData.getApp().getAppId())
            .waitId(activity.getUuid())
            .data(TaskData.builder()
                      .async(true)
                      .taskType(AWS_AMI_ASYNC_TASK.name())
                      .parameters(new Object[] {trafficShiftRequest})
                      .timeout(TimeUnit.MINUTES.toMillis(setupElement.getAutoScalingSteadyStateTimeout()))
                      .build())
            .tags(isNotEmpty(trafficShiftRequest.getAwsConfig().getTag())
                    ? singletonList(trafficShiftRequest.getAwsConfig().getTag())
                    : null)
            .envId(awsAmiTrafficShiftAlbData.getEnv().getUuid())
            .build();
    delegateService.queueTask(delegateTask);

    return executionData;
  }

  private AwsAmiTrafficShiftAlbSwitchRouteRequest buildAmiTrafficShiftSwitchRouteRequest(ExecutionContext context,
      AmiServiceTrafficShiftAlbSetupElement setupElement, AwsAmiTrafficShiftAlbData awsAmiTrafficShiftAlbData,
      Activity activity) {
    return AwsAmiTrafficShiftAlbSwitchRouteRequest.builder()
        .accountId(awsAmiTrafficShiftAlbData.getApp().getAccountId())
        .appId(awsAmiTrafficShiftAlbData.getApp().getUuid())
        .activityId(activity.getUuid())
        .commandName(SWAP_AUTO_SCALING_ROUTES)
        .oldAsgName(setupElement.getOldAutoScalingGroupName())
        .newAsgName(setupElement.getNewAutoScalingGroupName())
        .downscaleOldAsg(downsizeOldAsg)
        .rollback(isRollback())
        .newAutoscalingGroupWeight(getNewAutoscalingGroupWeight(context))
        .timeoutIntervalInMin(setupElement.getAutoScalingSteadyStateTimeout())
        .preDeploymentData(setupElement.getPreDeploymentData())
        .baseScalingPolicyJSONs(setupElement.getBaseScalingPolicyJSONs())
        .lbDetails(setupElement.getDetailsWithTargetGroups())
        .awsConfig(awsAmiTrafficShiftAlbData.getAwsConfig())
        .encryptionDetails(awsAmiTrafficShiftAlbData.getAwsEncryptedDataDetails())
        .region(awsAmiTrafficShiftAlbData.getRegion())
        .build();
  }

  protected List<CommandUnit> getCommandUnits() {
    return ImmutableList.of(new SpotinstDummyCommandUnit(SWAP_AUTO_SCALING_ROUTES),
        new SpotinstDummyCommandUnit(AwsConstants.DOWN_SCALE_ASG_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
  }

  protected int getNewAutoscalingGroupWeight(ExecutionContext context) {
    return spotinstStateHelper.renderCount(newAutoScalingGroupWeightExpr, context, DEFAULT_TRAFFIC_SHIFT_WEIGHT);
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(newAutoScalingGroupWeightExpr)) {
      invalidFields.put("newAutoScalingGroupWeightExpr", "New Autoscaling Group weight is needed");
    }
    return invalidFields;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  @Override
  public boolean isRollback() {
    return false;
  }

  @NotNull
  private Log.Builder getLogBuilder(Activity activity) {
    return aLog()
        .withAppId(activity.getAppId())
        .withActivityId(activity.getUuid())
        .withCommandUnitName(activity.getCommandUnits().get(0).getName());
  }

  private ExecutionResponse taskCreationFailureResponse(
      Activity activity, ManagerExecutionLogCallback executionLogCallback, Exception exception) {
    logger.error("Aws Ami traffic shift failed with error ", exception);
    Misc.logAllMessages(exception, executionLogCallback, CommandExecutionResult.CommandExecutionStatus.FAILURE);
    AwsAmiSetupExecutionData awsAmiExecutionData = AwsAmiSetupExecutionData.builder().build();
    String errorMessage = getMessage(exception);
    ExecutionResponseBuilder responseBuilder = ExecutionResponse.builder();
    return responseBuilder.correlationIds(singletonList(activity.getUuid()))
        .executionStatus(FAILED)
        .errorMessage(errorMessage)
        .stateExecutionData(awsAmiExecutionData)
        .async(true)
        .build();
  }

  private ExecutionResponse skipResponse() {
    return ExecutionResponse.builder()
        .executionStatus(SKIPPED)
        .errorMessage("No Aws Ami service setup element found. Skipping rollback.")
        .build();
  }

  private ExecutionResponse successResponse(Activity activity, AwsAmiTrafficShiftAlbStateExecutionData executionData) {
    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(executionData)
        .executionStatus(ExecutionStatus.SUCCESS)
        .correlationId(activity.getUuid())
        .build();
  }
}
