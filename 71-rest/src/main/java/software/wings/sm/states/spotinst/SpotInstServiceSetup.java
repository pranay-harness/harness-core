package software.wings.sm.states.spotinst;

import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import software.wings.beans.ResizeStrategy;
import software.wings.beans.TaskType;

import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.states.EcsStateHelper;

import java.util.Arrays;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class SpotInstServiceSetup extends State {
  public static final String SPOTINST_SERVICE_SETUP_COMMAND = "Spotinst Service Setup";

  @Getter @Setter private Integer targetListenerPort;
  @Getter @Setter private String targetListenerProtocol;
  @Getter @Setter private Integer maxInstances;
  @Getter @Setter private Integer currentRunningCount;
  @Getter @Setter private String elastiGroupNamePrefix;
  @Getter @Setter private boolean useLoadBalancer;
  @Getter @Setter private Integer timeoutIntervalInMin;
  @Getter @Setter private Integer olderActiveVersionCountToKeep;
  @Getter @Setter private boolean blueGreen;
  @Getter @Setter private boolean useCurrentRunningCount;
  @Getter @Setter private ResizeStrategy resizeStrategy;

  @Inject private transient AppService appService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient EcsStateHelper ecsStateHelper;
  @Inject private transient ActivityService activityService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
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
        spotInstTaskParameters.getTimeoutIntervalInMin() + 5);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(spotInstTaskParameters.getActivityId()))
        .withStateExecutionData(spotinstSetupStateExecutionData)
        .withAsync(true)
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
    stateExecutionData.setElastiGroupId(spotInstSetupTaskResponse.getNewElastiGroup().getId());
    stateExecutionData.setElastiGroupName(spotInstSetupTaskResponse.getNewElastiGroup().getName());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    SpotInstSetupContextElement spotInstSetupContextElement =
        SpotInstSetupContextElement.builder()
            .commandRequest(stateExecutionData.getSpotinstCommandRequest())
            .commandName(SPOTINST_SERVICE_SETUP_COMMAND)
            .maxInstanceCount(stateExecutionData.getMaxInstanceCount())
            .useCurrentRunningInstanceCount(stateExecutionData.isUseCurrentRunningInstanceCount())
            .currentRunningInstanceCount(stateExecutionData.getCurrentRunningInstanceCount())
            .resizeStrategy(resizeStrategy)
            .spotInstSetupTaskResponse(spotInstSetupTaskResponse)
            .isBlueGreen(blueGreen)
            .build();

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(spotInstSetupContextElement)
        .addNotifyElement(spotInstSetupContextElement)
        .build();
  }
}