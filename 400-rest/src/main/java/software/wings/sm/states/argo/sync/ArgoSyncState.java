package software.wings.sm.states.argo.sync;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.argo.ArgoCommandUnitConstants.ARGO_SYNC_COMMAND;
import static io.harness.argo.ArgoCommandUnitConstants.INIT;
import static io.harness.exception.ExceptionUtils.getMessage;

import static software.wings.sm.StateType.ARGO_SYNC;
import static software.wings.sm.states.argo.Constants.DEPLOYMENT_ERROR;
import static software.wings.sm.states.argo.Constants.PERFORM_DRIFT;
import static software.wings.sm.states.argo.Constants.PHASE_PARAM;

import static java.util.Collections.emptyMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.argo.ArgoCommandUnitConstants;
import io.harness.argo.beans.ArgoApp;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.argo.response.ArgoSyncResponse;
import io.harness.delegate.beans.argo.response.ArgoTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.ResponseData;

import software.wings.annotation.EncryptableSetting;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ArgoDummyCommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.settings.argo.ArgoConfig;
import software.wings.delegatetasks.argo.beans.request.ArgoAppSyncRequest;
import software.wings.delegatetasks.argo.beans.request.ArgoRequest;
import software.wings.infra.argo.ArgoAppConfig;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.argo.ArgoStateHelper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
@OwnedBy(CDP)
public class ArgoSyncState extends State {
  @Attributes(title = "Argo Name") @Getter @Setter private String argoName;
  @Inject private transient ActivityService activityService;
  @Inject ArgoStateHelper argoStateHelper;
  @Inject private transient DelegateService delegateService;
  @Inject private SweepingOutputService sweepingOutputService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;

  public ArgoSyncState(String name) {
    super(name, ARGO_SYNC.name());
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
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.fetchRequiredEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();

    DirectKubernetesInfrastructureMapping infrastructureMapping =
        (DirectKubernetesInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), context.fetchInfraMappingId());

    ArgoAppConfig argoAppConfig = infrastructureMapping.getArgoAppConfig();
    if (infrastructureMapping.getArgoAppConfig() == null) {
      throw new InvalidRequestException("Argo Config Is Not Set");
    }

    SettingAttribute settingAttribute =
        settingsService.getByAccount(app.getAccountId(), infrastructureMapping.getArgoConnectorId());
    if (settingAttribute == null) {
      throw new InvalidRequestException("Argo Config Is Not Set");
    }

    ArgoConfig argoConfig = (ArgoConfig) settingAttribute.getValue();
    Activity activity = argoStateHelper.createActivity(context, null, getStateType(), ARGO_SYNC_COMMAND,
        CommandUnitDetails.CommandUnitType.ARGO_DRIFT,
        ImmutableList.of(new ArgoDummyCommandUnit(INIT), new ArgoDummyCommandUnit(ARGO_SYNC_COMMAND)));

    ArgoSyncExecutionData argoSyncExecutionData = argoStateHelper.getArgoSyncExecutionData(
        app, env, infrastructureMapping, argoAppConfig, settingAttribute, argoConfig, activity, ARGO_SYNC_COMMAND);

    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
    ArgoRequest argoRequest = ArgoAppSyncRequest.builder()
                                  .argoConfig(argoConfig)
                                  .encryptedDataDetails(encryptedDataDetails)
                                  .appName(argoAppConfig.getAppName())
                                  .activityId(activity.getUuid())
                                  .appId(context.getAppId())
                                  .build();
    DelegateTask delegateTask = argoStateHelper.getDelegateTask(app, activity, env, infrastructureMapping, argoRequest,
        serviceElement, isSelectionLogsTrackingForTasksEnabled());

    delegateService.queueTask(delegateTask);
    appendDelegateTaskDetails(context, delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(activity.getUuid()))
        .stateExecutionData(argoSyncExecutionData)
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
    ArgoTaskResponse executionResponse = (ArgoTaskResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    ArgoSyncResponse argoSyncResponse = (ArgoSyncResponse) executionResponse;

    ArgoSyncExecutionData stateExecutionData = (ArgoSyncExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    if (ExecutionStatus.SUCCESS == executionStatus) {
      ArgoApp argoApp = argoSyncResponse.getArgoApp();
    }

    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .build();
  }

  @Override
  public Map<String, String> validateFields() {
    return emptyMap();
  }

  @Override
  public boolean isSelectionLogsTrackingForTasksEnabled() {
    return true;
  }
}
