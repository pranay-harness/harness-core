package software.wings.sm.states;

import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.ScriptType;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandType;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.SSHKeyDataProvider;
import software.wings.service.impl.WinRmConnectionAttributesDataProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
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
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Misc;
import software.wings.waitnotify.ErrorNotifyResponseData;
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
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;

  @Getter @Setter @Attributes(title = "Execute on Delegate") private boolean executeOnDelegate;

  @NotEmpty @Getter @Setter @Attributes(title = "Target Host") private String host;

  public enum ConnectionType { SSH, WINRM }

  @NotEmpty
  @Getter
  @Setter
  @DefaultValue("SSH")
  @Attributes(title = "Connection Type")
  private ConnectionType connectionType;

  @NotEmpty
  @Getter
  @Setter
  @Attributes(title = "SSH Key")
  @EnumData(enumDataProvider = SSHKeyDataProvider.class)
  @Property("sshKeyRef")
  private String sshKeyRef;

  @NotEmpty
  @Getter
  @Setter
  @Attributes(title = "Connection Attributes")
  @EnumData(enumDataProvider = WinRmConnectionAttributesDataProvider.class)
  private String connectionAttributes;

  @Getter @Setter @Attributes(title = "Working Directory") private String commandPath;

  @NotEmpty @Getter @Setter @DefaultValue("BASH") @Attributes(title = "Script Type") private ScriptType scriptType;

  @NotEmpty @Getter @Setter @Attributes(title = "Script") private String scriptString;

  /**
   * Create a new Script State with given name.
   *
   * @param name name of the state.
   */
  public ShellScriptState(String name) {
    super(name, StateType.SHELL_SCRIPT.name());
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    try {
      return executeInternal(context, activityId);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();

    NotifyResponseData data = response.values().iterator().next();

    if (data instanceof CommandExecutionResult) {
      CommandExecutionResult commandExecutionResult = (CommandExecutionResult) data;

      switch (commandExecutionResult.getStatus()) {
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
          throw new WingsException(
              "Unhandled type CommandExecutionStatus: " + commandExecutionResult.getStatus().name());
      }
      executionResponse.setErrorMessage(commandExecutionResult.getErrorMessage());

      ScriptStateExecutionData scriptStateExecutionData = (ScriptStateExecutionData) context.getStateExecutionData();
      scriptStateExecutionData.setDelegateMetaInfo(commandExecutionResult.getDelegateMetaInfo());
      executionResponse.setStateExecutionData(scriptStateExecutionData);
    } else if (data instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData executionData = (ErrorNotifyResponseData) data;
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage(((ErrorNotifyResponseData) data).getErrorMessage());
    } else {
      logger.error("Unhandled NotifyResponseData class " + data.getClass().getCanonicalName(), new Exception(""));
    }

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

    Map<String, String> serviceVariables = context.getServiceVariables();
    Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();

    if (serviceVariables != null) {
      serviceVariables.replaceAll((name, value) -> context.renderExpression(value));
    }

    if (safeDisplayServiceVariables != null) {
      safeDisplayServiceVariables.replaceAll((name, value) -> context.renderExpression(value));
    }

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    String username = null;
    WinRmConnectionAttributes winRmConnectionAttributes = null;
    List<EncryptedDataDetail> winrmEdd = Collections.emptyList();
    List<EncryptedDataDetail> keyEncryptionDetails = Collections.emptyList();

    if (connectionType == null) {
      connectionType = ConnectionType.SSH;
    }

    if (scriptType == null) {
      scriptType = ScriptType.BASH;
    }

    if (!executeOnDelegate) {
      if (connectionType == ConnectionType.SSH) {
        SettingAttribute keySettingAttribute = settingsService.get(sshKeyRef);
        HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) keySettingAttribute.getValue();
        username = hostConnectionAttributes.getUserName();
        keyEncryptionDetails = secretManager.getEncryptionDetails(
            (Encryptable) keySettingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

      } else if (connectionType == ConnectionType.WINRM) {
        winRmConnectionAttributes = (WinRmConnectionAttributes) settingsService.get(connectionAttributes).getValue();
        username = winRmConnectionAttributes.getUsername();
        winrmEdd = secretManager.getEncryptionDetails(
            winRmConnectionAttributes, context.getAppId(), context.getWorkflowExecutionId());
      }
    }

    ContainerServiceParams containerServiceParams = null;
    if (infrastructureMappingId != null) {
      InfrastructureMapping infraMapping =
          infrastructureMappingService.get(context.getAppId(), infrastructureMappingId);
      if (infraMapping instanceof ContainerInfrastructureMapping) {
        containerServiceParams = containerDeploymentManagerHelper.getContainerServiceParams(
            (ContainerInfrastructureMapping) infraMapping, "");
      }
    }
    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(TaskType.SCRIPT)
            .withAccountId(executionContext.getApp().getAccountId())
            .withWaitId(activityId)
            .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
            .withParameters(new Object[] {ShellScriptParameters.builder()
                                              .accountId(executionContext.getApp().getAccountId())
                                              .appId(executionContext.getAppId())
                                              .activityId(activityId)
                                              .host(context.renderExpression(host))
                                              .connectionType(connectionType)
                                              .winrmConnectionAttributes(winRmConnectionAttributes)
                                              .winrmConnectionEncryptedDataDetails(winrmEdd)
                                              .userName(username)
                                              .keyEncryptedDataDetails(keyEncryptionDetails)
                                              .containerServiceParams(containerServiceParams)
                                              .serviceVariables(serviceVariables)
                                              .safeDisplayServiceVariables(safeDisplayServiceVariables)
                                              .workingDirectory(commandPath)
                                              .scriptType(scriptType)
                                              .script(context.renderExpression(scriptString))
                                              .executeOnDelegate(executeOnDelegate)
                                              .build()})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(ScriptStateExecutionData.builder().activityId(activityId).build())
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

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    return asList(scriptString);
  }
}
