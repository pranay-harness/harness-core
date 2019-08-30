package software.wings.sm.states.spotinst;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.sm.StateType.SPOTINST_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.spotinst.model.ElastiGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.TaskType;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import java.util.Arrays;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class SpotInstServiceSetup extends State {
  public static final String SPOTINST_SERVICE_SETUP_COMMAND = "Spotinst Service Setup";
  public static final int DEFAULT_CURRENT_RUNNING_INSTANCE_COUNT = 2;

  @Getter @Setter private Integer minInstances;
  @Getter @Setter private Integer maxInstances;
  @Getter @Setter private Integer targetInstances;

  @Getter @Setter private Integer currentRunningCount;
  @Getter @Setter private String elastiGroupNamePrefix;
  @Getter @Setter private Integer timeoutIntervalInMin;
  @Getter @Setter private Integer olderActiveVersionCountToKeep;
  @Getter @Setter private boolean blueGreen;
  @Getter @Setter private boolean useCurrentRunningCount;
  @Getter @Setter private ResizeStrategy resizeStrategy;

  // LoadBalancer details
  @Getter @Setter private boolean useLoadBalancer;
  @Getter @Setter private String loadBalancerName;
  @Getter @Setter private boolean classicLoadBalancer;
  @Getter @Setter private String targetListenerPort;
  @Getter @Setter private String targetListenerProtocol;
  @Getter @Setter private String prodListenerPort;

  @Inject private transient ActivityService activityService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SpotInstStateHelper spotinstStateHelper;

  public SpotInstServiceSetup(String name) {
    super(name, SPOTINST_SETUP.name());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    // Override if not so.
    if (blueGreen) {
      resizeStrategy = ResizeStrategy.RESIZE_NEW_FIRST;
    }

    // Contains SpotInstCommandRequest + env/infra/activity/workflowExecution ids
    SpotInstSetupStateExecutionData spotinstSetupStateExecutionData =
        spotinstStateHelper.prepareStateExecutionData(context, this);

    SpotInstSetupTaskParameters spotInstTaskParameters =
        (SpotInstSetupTaskParameters) spotinstSetupStateExecutionData.getSpotinstCommandRequest()
            .getSpotInstTaskParameters();

    DelegateTask delegateTask = spotinstStateHelper.getDelegateTask(spotInstTaskParameters.getAccountId(),
        spotInstTaskParameters.getAppId(), TaskType.SPOTINST_COMMAND_TASK, spotInstTaskParameters.getActivityId(),
        spotinstSetupStateExecutionData.getEnvId(), spotinstSetupStateExecutionData.getInfraMappingId(),
        new Object[] {spotinstSetupStateExecutionData.getSpotinstCommandRequest()},
        spotinstStateHelper.generateTimeOutForDelegateTask(spotInstTaskParameters.getTimeoutIntervalInMin()));

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(spotInstTaskParameters.getActivityId()))
        .stateExecutionData(spotinstSetupStateExecutionData)
        .async(true)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    SpotInstTaskExecutionResponse executionResponse =
        (SpotInstTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    SpotInstSetupTaskResponse spotInstSetupTaskResponse =
        (SpotInstSetupTaskResponse) executionResponse.getSpotInstTaskResponse();

    SpotInstSetupStateExecutionData stateExecutionData =
        (SpotInstSetupStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    if (ExecutionStatus.SUCCESS.equals(executionStatus) && spotInstSetupTaskResponse != null) {
      if (spotInstSetupTaskResponse.getNewElastiGroup() != null) {
        stateExecutionData.getElastiGroupOriginalConfig().setId(spotInstSetupTaskResponse.getNewElastiGroup().getId());
        stateExecutionData.getElastiGroupOriginalConfig().setName(
            spotInstSetupTaskResponse.getNewElastiGroup().getName());
        stateExecutionData.setElastiGroupId(spotInstSetupTaskResponse.getNewElastiGroup().getId());
        stateExecutionData.setElastiGroupName(spotInstSetupTaskResponse.getNewElastiGroup().getName());
      }
    }

    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    SpotInstCommandRequest spotInstCommandRequest = stateExecutionData.getSpotinstCommandRequest();
    SpotInstSetupContextElement spotInstSetupContextElement =
        SpotInstSetupContextElement.builder()
            .commandRequest(spotInstCommandRequest)
            .commandName(SPOTINST_SERVICE_SETUP_COMMAND)
            .maxInstanceCount(stateExecutionData.getMaxInstanceCount())
            .useCurrentRunningInstanceCount(stateExecutionData.isUseCurrentRunningInstanceCount())
            .resizeStrategy(resizeStrategy)
            .spotInstSetupTaskResponse(spotInstSetupTaskResponse)
            .isBlueGreen(blueGreen)
            .serviceId(stateExecutionData.getServiceId())
            .infraMappingId(stateExecutionData.getInfraMappingId())
            .appId(stateExecutionData.getAppId())
            .envId(stateExecutionData.getEnvId())
            .newElastiGroupOriginalConfig(stateExecutionData.getElastiGroupOriginalConfig())
            .elstiGroupNamePrefix(context.renderExpression(elastiGroupNamePrefix))
            .build();

    // Add these details only if spotInstSetupTaskResponse is not NULL
    addDetailsForSuccessfulExecution(spotInstSetupContextElement, spotInstSetupTaskResponse);

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .contextElement(spotInstSetupContextElement)
        .notifyElement(spotInstSetupContextElement)
        .build();
  }

  private void addDetailsForSuccessfulExecution(
      SpotInstSetupContextElement spotInstSetupContextElement, SpotInstSetupTaskResponse spotInstSetupTaskResponse) {
    if (spotInstSetupTaskResponse == null) {
      return;
    }

    ElastiGroup oldElastiGroup = fetchOldElasticGroup(spotInstSetupTaskResponse);
    spotInstSetupContextElement.setOldElastiGroupOriginalConfig(oldElastiGroup);
    spotInstSetupContextElement.setProdListenerArn(spotInstSetupTaskResponse.getProdListenerArn());
    spotInstSetupContextElement.setStageListenerArn(spotInstSetupTaskResponse.getStageListenerArn());
    spotInstSetupContextElement.setStageTargetGroupArn(spotInstSetupTaskResponse.getStageTargetGroupArn());
    spotInstSetupContextElement.setProdTargetGroupArn(spotInstSetupTaskResponse.getProdTargetGroupArn());

    if (oldElastiGroup != null && oldElastiGroup.getCapacity() != null) {
      spotInstSetupContextElement.setCurrentRunningInstanceCount(oldElastiGroup.getCapacity().getTarget());
    } else {
      spotInstSetupContextElement.setCurrentRunningInstanceCount(DEFAULT_CURRENT_RUNNING_INSTANCE_COUNT);
    }
  }

  private ElastiGroup fetchOldElasticGroup(SpotInstSetupTaskResponse spotInstSetupTaskResponse) {
    if (isEmpty(spotInstSetupTaskResponse.getGroupToBeDownsized())) {
      return null;
    }

    return spotInstSetupTaskResponse.getGroupToBeDownsized().get(0);
  }
}