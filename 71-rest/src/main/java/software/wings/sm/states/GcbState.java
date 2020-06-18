package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.beans.TaskData.asyncTaskData;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.TaskType.GCB;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.POLL;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.START;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.context.ContextElementType;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.GcbExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GcpConfig;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;
import software.wings.stencils.DefaultValue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@OwnedBy(CDC)
@Slf4j
public class GcbState extends State implements SweepingOutputStateMixin {
  public static final String GCB_LOGS = "GCB Output";

  @Getter @Setter private List<ParameterEntry> jobParameters = Lists.newArrayList();

  @Getter @Setter private String gcpConfigId;
  @Getter @Setter private String gcbBuildUrl;
  @Getter @Setter private String projectId; // extract
  @Getter @Setter private String triggerId;
  @Getter @Setter private String branchName;
  @Getter @Setter private String sweepingOutputName;
  @Getter @Setter private SweepingOutputInstance.Scope sweepingOutputScope;

  @Transient @Inject private DelegateService delegateService;
  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private SecretManager secretManager;
  @Transient @Inject private SweepingOutputService sweepingOutputService;

  public GcbState(String name) {
    super(name, StateType.GCB.name());
  }

  @Override
  @Attributes(title = "Wait interval before execution (s)")
  public Integer getWaitInterval() {
    return super.getWaitInterval();
  }

  @Attributes(title = "Execute with previous steps")
  public boolean getExecuteWithPreviousSteps() {
    return super.isExecuteWithPreviousSteps();
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis() != null ? super.getTimeoutMillis() : Math.toIntExact(DEFAULT_ASYNC_CALL_TIMEOUT);
  }

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    return Stream.of(jobParameters)
        .flatMap(List::stream)
        .map(ParameterEntry::getValue)
        .collect(toCollection(LinkedList::new));
  }

  @Override
  public ExecutionResponse execute(final @NotNull ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  /**
   * Execute internal execution response.
   *
   * @param context the context
   * @return the execution response
   */
  protected ExecutionResponse executeInternal(
      final @NotNull ExecutionContext context, final @NotNull String activityId) {
    Map<String, String> evaluatedParameters =
        Stream.of(jobParameters)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(toMap(ParameterEntry::getKey,
                entry -> context.renderExpression(entry.getValue()), CollectionUtils::overrideOperator, HashMap::new));

    final Application application = context.fetchRequiredApp();
    final String appId = application.getAppId();
    final GcpConfig config = context.getGlobalSettingValue(context.getAccountId(), gcpConfigId);
    GcbTaskParams gcbTaskParams = GcbTaskParams.builder()
                                      .gcpConfig(config)
                                      .type(START)
                                      .projectId(projectId)
                                      .triggerId(triggerId)
                                      .branchName(branchName)
                                      .encryptedDataDetails(secretManager.getEncryptionDetails(
                                          config, context.getAppId(), context.getWorkflowExecutionId()))
                                      .parameters(evaluatedParameters)
                                      .activityId(activityId)
                                      .buildUrl(gcbBuildUrl)
                                      .unitName(GCB_LOGS)
                                      .appId(appId)
                                      .build();

    if (getTimeoutMillis() != null) {
      gcbTaskParams.setTimeout(getTimeoutMillis());
      gcbTaskParams.setStartTs(System.currentTimeMillis());
    }

    final String delegateTaskId = delegateService.queueTask(delegateTaskOf(activityId, context, gcbTaskParams));

    GcbExecutionData gcbExecutionData =
        GcbExecutionData.builder().activityId(activityId).jobParameters(evaluatedParameters).build();

    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(gcbExecutionData)
        .correlationIds(singletonList(activityId))
        .delegateTaskId(delegateTaskId)
        .build();
  }

  private static DelegateTask delegateTaskOf(
      @NotNull final String activityId, @NotNull final ExecutionContext context, Object... parameters) {
    final Application application = context.fetchRequiredApp();
    final WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = Optional.ofNullable(workflowStandardParams)
                       .map(WorkflowStandardParams::getEnv)
                       .map(Environment::getUuid)
                       .orElse(null);
    return DelegateTask.builder()
        .accountId(application.getAccountId())
        .waitId(activityId)
        .appId(application.getAppId())
        .data(asyncTaskData(GCB.name(), parameters))
        .envId(envId)
        .infrastructureMappingId(context.fetchInfraMappingId())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage())
          .build();
    }

    GcbDelegateResponse gcbDelegateResponse = (GcbDelegateResponse) notifyResponseData;
    if (gcbDelegateResponse.isWorking()) {
      return startPollTask(context, gcbDelegateResponse);
    }
    final GcbExecutionData gcbExecutionData = context.getStateExecutionData();
    activityService.updateStatus(gcbDelegateResponse.getParams().getActivityId(), context.getAppId(), SUCCESS);

    handleSweepingOutput(sweepingOutputService, context, gcbExecutionData);

    return ExecutionResponse.builder()
        .executionStatus(gcbDelegateResponse.getStatus())
        .stateExecutionData(gcbExecutionData)
        .build();
  }

  protected ExecutionResponse startPollTask(ExecutionContext context, GcbDelegateResponse delegateResponse) {
    GcbTaskParams parameters = delegateResponse.getParams();
    parameters.setType(POLL);
    final String waitId = UUIDGenerator.generateUuid();
    final String delegateTaskId = delegateService.queueTask(delegateTaskOf(waitId, context, parameters));
    final GcbExecutionData gcbExecutionData = context.getStateExecutionData();
    return ExecutionResponse.builder()
        .async(true)
        .delegateTaskId(delegateTaskId)
        .correlationIds(singletonList(waitId))
        .stateExecutionData(gcbExecutionData)
        .executionStatus(RUNNING)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    if (context == null || context.getStateExecutionData() == null) {
      return;
    }
    context.getStateExecutionData().setErrorMsg(
        "Job did not complete within timeout " + (getTimeoutMillis() / 1000) + " (s)");
  }

  @NotNull
  protected String createActivity(ExecutionContext executionContext) {
    return activityService.save(new Activity().with(this).with(executionContext).with(CommandUnitType.GCB)).getUuid();
  }

  @Data
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class GcbDelegateResponse implements DelegateTaskNotifyResponseData {
    @NotNull private ExecutionStatus status;
    @NotNull private GcbBuildDetails build;
    @NotNull private GcbTaskParams params;
    @Nullable private DelegateMetaInfo delegateMetaInfo;

    @NotNull
    public static GcbDelegateResponse gcbDelegateResponseOf(
        @NotNull final GcbTaskParams params, @NotNull final GcbBuildDetails build) {
      return new GcbDelegateResponse(build.getStatus().getExecutionStatus(), build, params, null);
    }

    public boolean isWorking() {
      return build.isWorking();
    }
  }
}
