package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.TaskType.GCB;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.CANCEL;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.POLL;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.START;
import static software.wings.helpers.ext.gcb.models.GcbBuildStatus.CANCELLED;
import static software.wings.sm.states.gcbconfigs.GcbOptions.GcbSpecSource.REMOTE;
import static software.wings.sm.states.gcbconfigs.GcbOptions.GcbSpecSource.TRIGGER;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
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
import io.harness.delegate.beans.TaskData;
import io.harness.exception.UnsupportedOperationException;
import io.harness.tasks.Cd1SetupFields;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import software.wings.beans.GitConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.GcbTaskParams;
import software.wings.beans.template.TemplateUtils;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.gcbconfigs.GcbOptions;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;
import software.wings.stencils.DefaultValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@OwnedBy(CDC)
public class GcbState extends State implements SweepingOutputStateMixin {
  public static final String GCB_LOGS = "GCB Output";

  @Getter @Setter private GcbOptions gcbOptions;
  @Getter @Setter private String sweepingOutputName;
  @Getter @Setter private SweepingOutputInstance.Scope sweepingOutputScope;

  @Transient @Inject private DelegateService delegateService;
  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private SecretManager secretManager;
  @Transient @Inject private SweepingOutputService sweepingOutputService;
  @Transient @Inject private TemplateExpressionProcessor templateExpressionProcessor;
  @Transient @Inject private TemplateUtils templateUtils;
  @Transient @Inject private SettingsService settingsService;

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
  public ExecutionResponse execute(final @NotNull ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  Map<String, String> evaluate(
      @NotNull final ExecutionContext context, @Nullable final List<NameValuePair> parameters) {
    return Stream.of(parameters)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .collect(toMap(NameValuePair::getName,
            entry -> context.renderExpression(entry.getValue()), CollectionUtils::overrideOperator, HashMap::new));
  }

  /**
   * Execute internal execution response.
   *
   * @param context the context
   * @return the execution response
   */
  protected ExecutionResponse executeInternal(
      final @NotNull ExecutionContext context, final @NotNull String activityId) {
    resolveGcbOptionExpressions(context);

    final Application application = context.fetchRequiredApp();
    final String appId = application.getAppId();
    Map<String, String> substitutions = null;
    if (gcbOptions.getSpecSource() == TRIGGER && !isEmpty(gcbOptions.getTriggerSpec().getSubstitutions())) {
      substitutions = evaluate(context, gcbOptions.getTriggerSpec().getSubstitutions());
    }
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression gcpConfigExp =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "gcpConfigId");
      TemplateExpression gitConfigExp =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "gitConfigId");
      if (gcpConfigExp != null && !resolveGcpTemplateExpression(gcpConfigExp, context)) {
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("Google Cloud Provider does not exist. Please update with an appropriate cloud provider.")
            .build();
      }
      if (gitConfigExp != null && !resolveGitTemplateExpression(gitConfigExp, context)) {
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("Git connector does not exist. Please update with an appropriate git connector.")
            .build();
      }
    }
    GcpConfig gcpConfig = context.getGlobalSettingValue(context.getAccountId(), gcbOptions.getGcpConfigId());

    GitConfig gitConfig = gcbOptions.getSpecSource() == REMOTE
        ? context.getGlobalSettingValue(context.getAccountId(), gcbOptions.getRepositorySpec().getGitConfigId())
        : null;

    GcbTaskParams gcbTaskParams = GcbTaskParams.builder()
                                      .gcpConfig(gcpConfig)
                                      .type(START)
                                      .gcbOptions(gcbOptions)
                                      .substitutions(substitutions)
                                      .encryptedDataDetails(secretManager.getEncryptionDetails(
                                          gcpConfig, context.getAppId(), context.getWorkflowExecutionId()))
                                      .activityId(activityId)
                                      .unitName(GCB_LOGS)
                                      .gitConfig(gitConfig)
                                      .appId(appId)
                                      .build();

    if (getTimeoutMillis() != null) {
      gcbTaskParams.setTimeout(getTimeoutMillis());
      gcbTaskParams.setStartTs(System.currentTimeMillis());
    }
    DelegateTask delegateTask = delegateTaskOf(activityId, context, gcbTaskParams);
    delegateService.queueTask(delegateTask);

    GcbExecutionData gcbExecutionData = GcbExecutionData.builder().activityId(activityId).build();
    gcbExecutionData.setTemplateVariable(templateUtils.processTemplateVariables(context, getTemplateVariables()));
    appendDelegateTaskDetails(context, delegateTask);
    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(gcbExecutionData)
        .correlationIds(singletonList(activityId))
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
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, application.getAppId())
        .data(TaskData.builder()
                  .async(true)
                  .taskType(GCB.name())
                  .parameters(parameters)
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, envId)
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, context.fetchInfraMappingId())
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

    GcbDelegateResponse delegateResponse = (GcbDelegateResponse) notifyResponseData;
    if (delegateResponse.isWorking()) {
      return startPollTask(context, delegateResponse);
    }
    final GcbExecutionData gcbExecutionData = context.getStateExecutionData();
    activityService.updateStatus(
        delegateResponse.getParams().getActivityId(), context.getAppId(), delegateResponse.getStatus());

    handleSweepingOutput(sweepingOutputService, context, gcbExecutionData.withDelegateResponse(delegateResponse));

    return ExecutionResponse.builder()
        .executionStatus(delegateResponse.getStatus())
        .stateExecutionData(gcbExecutionData)
        .build();
  }

  protected ExecutionResponse startPollTask(ExecutionContext context, GcbDelegateResponse delegateResponse) {
    GcbTaskParams parameters = delegateResponse.getParams();
    parameters.setType(POLL);
    final String waitId = UUIDGenerator.generateUuid();
    DelegateTask delegateTask = delegateTaskOf(waitId, context, parameters);
    delegateService.queueTask(delegateTask);
    appendDelegateTaskDetails(context, delegateTask);
    final GcbExecutionData gcbExecutionData = context.getStateExecutionData();
    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(waitId))
        .stateExecutionData(gcbExecutionData.withDelegateResponse(delegateResponse))
        .executionStatus(RUNNING)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    GcbTaskParams params =
        GcbTaskParams.builder()
            .type(CANCEL)
            .gcpConfig(
                (GcpConfig) settingsService.get(((GcbExecutionData) context.getStateExecutionData()).getGcpConfigId())
                    .getValue())
            .accountId(context.getAccountId())
            .buildId(String.valueOf(context.getStateExecutionData().getExecutionDetails().get("buildNo").getValue()))
            .encryptedDataDetails(secretManager.getEncryptionDetails(
                (GcpConfig) settingsService.get(((GcbExecutionData) context.getStateExecutionData()).getGcpConfigId())
                    .getValue(),
                context.getAppId(), context.getWorkflowExecutionId()))
            .build();
    delegateService.queueTask(
        delegateTaskOf(((GcbExecutionData) context.getStateExecutionData()).getActivityId(), context, params));
    ((GcbExecutionData) context.getStateExecutionData()).setBuildStatus(CANCELLED);
  }

  @NotNull
  protected String createActivity(ExecutionContext executionContext) {
    return activityService.save(new Activity().with(this).with(executionContext).with(CommandUnitType.GCB)).getUuid();
  }

  @VisibleForTesting
  protected void resolveGcbOptionExpressions(ExecutionContext context) {
    switch (gcbOptions.getSpecSource()) {
      case TRIGGER:
        gcbOptions.getTriggerSpec().setSourceId(context.renderExpression(gcbOptions.getTriggerSpec().getSourceId()));
        gcbOptions.getTriggerSpec().setName(context.renderExpression(gcbOptions.getTriggerSpec().getName()));
        break;
      case INLINE:
        gcbOptions.setInlineSpec(context.renderExpression(gcbOptions.getInlineSpec()));
        break;
      case REMOTE:
        gcbOptions.getRepositorySpec().setSourceId(
            context.renderExpression(gcbOptions.getRepositorySpec().getSourceId()));
        gcbOptions.getRepositorySpec().setFilePath(
            context.renderExpression(gcbOptions.getRepositorySpec().getFilePath()));
        break;
      default:
        throw new UnsupportedOperationException("Gcb option " + gcbOptions.getSpecSource() + " not supported");
    }
  }

  @Data
  @RequiredArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class GcbDelegateResponse implements DelegateTaskNotifyResponseData {
    @NotNull private final ExecutionStatus status;
    @NotNull private final GcbBuildDetails build;
    @NotNull private final GcbTaskParams params;
    @Nullable private DelegateMetaInfo delegateMetaInfo;

    @NotNull
    public static GcbDelegateResponse gcbDelegateResponseOf(
        @NotNull final GcbTaskParams params, @NotNull final GcbBuildDetails build) {
      return new GcbDelegateResponse(build.getStatus().getExecutionStatus(), build, params);
    }

    public boolean isWorking() {
      return build.isWorking();
    }
  }

  @VisibleForTesting
  protected boolean resolveGcpTemplateExpression(TemplateExpression gcpConfigExp, ExecutionContext context) {
    if (gcpConfigExp != null) {
      String resolvedExpression = templateExpressionProcessor.resolveTemplateExpression(context, gcpConfigExp);
      GcpConfig gcpConfig = context.getGlobalSettingValue(context.getAccountId(), resolvedExpression);
      if (gcpConfig != null) {
        gcbOptions.setGcpConfigId(resolvedExpression);
      } else {
        SettingAttribute setting =
            settingsService.getSettingAttributeByName(context.getAccountId(), resolvedExpression);
        if (setting == null) {
          return false;
        }
        gcbOptions.setGcpConfigId(setting.getUuid());
      }
    }
    return true;
  }

  @VisibleForTesting
  protected boolean resolveGitTemplateExpression(TemplateExpression gitConfigExp, ExecutionContext context) {
    if (gitConfigExp != null) {
      String resolvedExpression = templateExpressionProcessor.resolveTemplateExpression(context, gitConfigExp);
      GitConfig gitConfig = context.getGlobalSettingValue(context.getAccountId(), resolvedExpression);
      if (gitConfig != null) {
        gcbOptions.getRepositorySpec().setGitConfigId(resolvedExpression);
      } else {
        SettingAttribute setting =
            settingsService.getSettingAttributeByName(context.getAccountId(), resolvedExpression);
        if (setting == null) {
          return false;
        }
        gcbOptions.getRepositorySpec().setGitConfigId(setting.getUuid());
      }
    }
    return true;
  }
}
