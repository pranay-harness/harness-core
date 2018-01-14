package software.wings.sm.states;

import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ScriptStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandType;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.SSHKeyDataProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ShellScriptState extends State {
  private static final Logger logger = LoggerFactory.getLogger(ShellScriptState.class);
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient SecretManager secretManager;

  @NotEmpty @Getter @Setter @Attributes(title = "Target Host") private String host;

  @NotEmpty
  @Getter
  @Setter
  @Attributes(title = "SSH Key")
  @EnumData(enumDataProvider = SSHKeyDataProvider.class)
  private String sshKeyRef;

  @Getter @Setter @Attributes(title = "Working Directory") private String commandPath;

  @NotEmpty @Getter @Setter @Attributes(title = "Script") private String scriptString;

  /**
   * Create a new Script State with given name.
   *
   * @param name name of the state.
   */
  public ShellScriptState(String name) {
    super(name, StateType.SHELL_SCRIPT.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    CommandExecutionResult executionData = (CommandExecutionResult) response.values().iterator().next();

    ExecutionResponse executionResponse = new ExecutionResponse();

    switch (executionData.getStatus()) {
      case SUCCESS:
        executionResponse.setExecutionStatus(ExecutionStatus.SUCCESS);
        break;
      case FAILURE:
        executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
        break;
      case RUNNING:
        executionResponse.setExecutionStatus(ExecutionStatus.RUNNING);
        break;
      case QUEUED:
        executionResponse.setExecutionStatus(ExecutionStatus.QUEUED);
        break;
      default:
        throw new WingsException("Unhandled type CommandExecutionStatus: " + executionData.getStatus().name());
    }
    executionResponse.setErrorMessage(executionData.getErrorMessage());

    String activityId = null;

    for (String key : response.keySet()) {
      activityId = key;
    }

    updateActivityStatus(
        activityId, ((ExecutionContextImpl) context).getApp().getUuid(), executionResponse.getExecutionStatus());

    return executionResponse;
  }

  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = (workflowStandardParams == null || workflowStandardParams.getEnv() == null)
        ? null
        : workflowStandardParams.getEnv().getUuid();

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    SettingAttribute keySettingAttribute = settingsService.get(sshKeyRef);
    List<EncryptedDataDetail> keyEncryptionDetails = secretManager.getEncryptionDetails(
        (Encryptable) keySettingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    String delegateTaskId = delegateService.queueTask(
        aDelegateTask()
            .withTaskType(TaskType.SCRIPT)
            .withAccountId(executionContext.getApp().getAccountId())
            .withWaitId(activityId)
            .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
            .withParameters(new Object[] {ShellScriptParameters.builder()
                                              .accountId(executionContext.getApp().getAccountId())
                                              .appId(executionContext.getAppId())
                                              .activityId(activityId)
                                              .host(host)
                                              .keyEncryptedDataDetails(keyEncryptionDetails)
                                              .script(scriptString)
                                              .build()})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(ScriptStateExecutionData.builder().name("foo").activityId(activityId).build())
        .build();
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .commandName(getName())
                                          .type(Activity.Type.Verification)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(asList(Command.Builder.aCommand()
                                                                   .withName(ShellScriptParameters.CommandUnit)
                                                                   .withCommandType(CommandType.OTHER)
                                                                   .build()))
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING);

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }

    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    if (instanceElement != null && instanceElement.getServiceTemplateElement() != null) {
      activityBuilder.serviceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .serviceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .serviceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .serviceInstanceId(instanceElement.getUuid())
          .hostName(instanceElement.getHost().getHostName());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }
}
