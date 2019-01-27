package software.wings.sm.states;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.template.TemplateHelper.convertToVariableMap;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.delegates.beans.ScriptType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.serializer.KryoUtils;
import io.harness.waiter.ErrorNotifyResponseData;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ScriptStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SweepingOutput;
import software.wings.beans.TaskType;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.common.Constants;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.SSHKeyDataProvider;
import software.wings.service.impl.WinRmConnectionAttributesDataProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Misc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShellScriptState extends State {
  private static final Logger logger = LoggerFactory.getLogger(ShellScriptState.class);
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject @Transient private SweepingOutputService sweepingOutputService;

  @Getter @Setter @Attributes(title = "Execute on Delegate") private boolean executeOnDelegate;

  @NotEmpty @Getter @Setter @Attributes(title = "Target Host") private String host;
  @NotEmpty @Getter @Setter @Attributes(title = "Tags") private List<String> tags;

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

  @Getter @Setter private String outputVars;
  @Getter @Setter private SweepingOutput.Scope sweepingOutputScope;
  @Getter @Setter private String sweepingOutputName;

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
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    String activityId = response.keySet().iterator().next();
    ResponseData data = response.values().iterator().next();
    boolean saveSweepingOutputToContext = false;
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
      if (commandExecutionResult.getStatus().equals(SUCCESS)) {
        Map<String, String> sweepingOutputEnvVariables =
            ((ShellExecutionData) ((CommandExecutionResult) data).getCommandExecutionData())
                .getSweepingOutputEnvVariables();
        scriptStateExecutionData.setSweepingOutputEnvVariables(sweepingOutputEnvVariables);
        saveSweepingOutputToContext = true;
      }
      executionResponse.setStateExecutionData(scriptStateExecutionData);
    } else if (data instanceof ErrorNotifyResponseData) {
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage(((ErrorNotifyResponseData) data).getErrorMessage());
      return executionResponse;
    } else {
      logger.error("Unhandled ResponseData class " + data.getClass().getCanonicalName(), new Exception(""));
    }

    updateActivityStatus(
        activityId, ((ExecutionContextImpl) context).getApp().getUuid(), executionResponse.getExecutionStatus());
    if (isNotEmpty(sweepingOutputName) && saveSweepingOutputToContext) {
      final SweepingOutput sweepingOutput =
          context.prepareSweepingOutputBuilder(sweepingOutputScope)
              .name(sweepingOutputName)
              .output(KryoUtils.asDeflatedBytes(
                  ((ShellExecutionData) ((CommandExecutionResult) data).getCommandExecutionData())
                      .getSweepingOutputEnvVariables()))
              .build();

      sweepingOutputService.save(sweepingOutput);
    }
    return executionResponse;
  }

  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    ScriptStateExecutionData scriptStateExecutionData =
        ScriptStateExecutionData.builder().activityId(activityId).build();
    scriptStateExecutionData.setTemplateVariable(convertToVariableMap(getTemplateVariables()));
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = (workflowStandardParams == null || workflowStandardParams.getEnv() == null)
        ? null
        : workflowStandardParams.getEnv().getUuid();

    String appId = workflowStandardParams == null ? null : workflowStandardParams.getAppId();
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infrastructureMappingId);
    String serviceTemplateId = infrastructureMapping == null ? null : infrastructureMapping.getServiceTemplateId();

    Map<String, String> serviceVariables = context.getServiceVariables().entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();

    if (serviceVariables != null) {
      serviceVariables.replaceAll((name, value) -> context.renderExpression(value));
    }

    if (safeDisplayServiceVariables != null) {
      safeDisplayServiceVariables.replaceAll((name, value) -> context.renderExpression(value));
    }

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    String username = null;
    String keyPath = null;
    boolean keyless = false;
    Integer port = null;
    HostConnectionAttributes.AccessType accessType = null;
    String keyName = null;
    WinRmConnectionAttributes winRmConnectionAttributes = null;
    List<EncryptedDataDetail> winrmEdd = emptyList();
    List<EncryptedDataDetail> keyEncryptionDetails = emptyList();

    HostConnectionAttributes hostConnectionAttributes = null;

    if (connectionType == null) {
      connectionType = ConnectionType.SSH;
    }

    if (scriptType == null) {
      scriptType = ScriptType.BASH;
    }

    if (!executeOnDelegate) {
      if (connectionType == ConnectionType.SSH) {
        if (isEmpty(sshKeyRef)) {
          logger.warn("SSH Connection Attribute not provided in Shell Script Step");
          throw new WingsException("SSH Connection Attribute not provided in Shell Script Step");
        }
        SettingAttribute keySettingAttribute = settingsService.get(sshKeyRef);
        if (keySettingAttribute == null) {
          logger.warn("SSH Connection Attribute provided in Shell Script Step not found");
          throw new WingsException("SSH Connection Attribute provided in Shell Script Step not found");
        }
        hostConnectionAttributes = (HostConnectionAttributes) keySettingAttribute.getValue();
        username = ((HostConnectionAttributes) keySettingAttribute.getValue()).getUserName();
        keyPath = ((HostConnectionAttributes) keySettingAttribute.getValue()).getKeyPath();
        keyless = ((HostConnectionAttributes) keySettingAttribute.getValue()).isKeyless();
        port = ((HostConnectionAttributes) keySettingAttribute.getValue()).getSshPort();
        if (port == null) {
          port = 22;
        }
        accessType = ((HostConnectionAttributes) keySettingAttribute.getValue()).getAccessType();
        keyName = keySettingAttribute.getUuid();
        keyEncryptionDetails = secretManager.getEncryptionDetails(
            (EncryptableSetting) keySettingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

      } else if (connectionType == ConnectionType.WINRM) {
        if (isEmpty(connectionAttributes)) {
          logger.warn("WinRM Connection Attribute not provided in Shell Script Step");
          throw new WingsException("WinRM Connection Attribute not provided in Shell Script Step");
        }
        winRmConnectionAttributes = (WinRmConnectionAttributes) settingsService.get(connectionAttributes).getValue();
        if (winRmConnectionAttributes == null) {
          logger.warn("WinRM Connection Attribute provided in Shell Script Step not found");
          throw new WingsException("WinRM Connection Attribute provided in Shell Script Step not found");
        }
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
            (ContainerInfrastructureMapping) infraMapping, "", context);
      }
    }

    if (StringUtils.isEmpty(commandPath)) {
      if (scriptType.equals(ScriptType.BASH)) {
        commandPath = "/tmp";
      } else if (scriptType.equals(ScriptType.POWERSHELL)) {
        commandPath = "%TEMP%";
      }
    }

    List<String> allTags = newArrayList();
    String cloudProviderTag = getTagFromCloudProvider(containerServiceParams);
    if (isNotEmpty(cloudProviderTag)) {
      allTags.add(cloudProviderTag);
    }
    if (isNotEmpty(tags)) {
      allTags.addAll(tags);
    }

    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(TaskType.SCRIPT)
            .withAccountId(executionContext.getApp().getAccountId())
            .withWaitId(activityId)
            .withTags(allTags)
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
                                              .script(scriptString)
                                              .executeOnDelegate(executeOnDelegate)
                                              .outputVars(outputVars)
                                              .hostConnectionAttributes(hostConnectionAttributes)
                                              .keyless(keyless)
                                              .keyPath(keyPath)
                                              .port(port)
                                              .accessType(accessType)
                                              .keyName(keyName)
                                              .build()})
            .withEnvId(envId)
            .withInfrastructureMappingId(infrastructureMappingId)
            .withServiceTemplateId(serviceTemplateId)
            .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }

    String delegateTaskId = scheduleDelegateTask(context, delegateTask, scriptStateExecutionData);
    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(scriptStateExecutionData)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private String getTagFromCloudProvider(ContainerServiceParams containerServiceParams) {
    if (containerServiceParams != null) {
      SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
      SettingValue settingValue = settingAttribute.getValue();
      if (settingValue instanceof AwsConfig) {
        return ((AwsConfig) settingValue).getTag();
      }
    }
    return null;
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
    return asList(scriptString, host);
  }
}
