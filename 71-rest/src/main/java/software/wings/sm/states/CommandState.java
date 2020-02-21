package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.eraro.ErrorCode.COMMAND_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.joor.Reflect.on;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.sm.StateType.COMMAND;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.beans.TaskType;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.CleanupPowerShellCommandUnit;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitPowerShellCommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.beans.command.InitSshCommandUnitV2;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.command.TailFilePatternEntry;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.template.ReferencedTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateUtils;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateExecutionContext.StateExecutionContextBuilder;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.Expand;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
@Slf4j
public class CommandState extends State {
  public static final String RUNTIME_PATH = "RUNTIME_PATH";
  public static final String BACKUP_PATH = "BACKUP_PATH";
  public static final String STAGING_PATH = "STAGING_PATH";
  public static final String WINDOWS_RUNTIME_PATH = "WINDOWS_RUNTIME_PATH";

  @Inject @Transient @SchemaIgnore private transient ExecutorService executorService;
  @Inject @Transient private transient ActivityHelperService activityHelperService;
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient AppService appService;
  @Inject @Transient private transient ArtifactService artifactService;
  @Inject @Transient private transient ArtifactStreamService artifactStreamService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient HostService hostService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private transient ServiceInstanceService serviceInstanceService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;
  @Inject @Transient private transient FeatureFlagService featureFlagService;
  @Inject @Transient private transient AwsCommandHelper awsCommandHelper;
  @Inject @Transient private transient TemplateService templateService;
  @Inject @Transient private transient TemplateUtils templateUtils;
  @Inject @Transient private transient ServiceTemplateHelper serviceTemplateHelper;

  @Attributes(title = "Command") @Expand(dataProvider = CommandStateEnumDataProvider.class) private String commandName;

  @NotEmpty @SchemaIgnore private String sshKeyRef;
  @NotEmpty @SchemaIgnore private String connectionAttributes;

  @SchemaIgnore private boolean executeOnDelegate;

  @NotEmpty @SchemaIgnore private String host;

  public enum ConnectionType { SSH, WINRM }

  @NotEmpty @SchemaIgnore private ConnectionType connectionType;

  @SchemaIgnore private static String artifactFileName;

  @SchemaIgnore
  public String getSshKeyRef() {
    return sshKeyRef;
  }

  public void setSshKeyRef(String sshKeyRef) {
    this.sshKeyRef = sshKeyRef;
  }

  @SchemaIgnore
  public String getConnectionAttributes() {
    return connectionAttributes;
  }

  public void setConnectionAttributes(String connectionAttributes) {
    this.connectionAttributes = connectionAttributes;
  }

  @SchemaIgnore
  public boolean isExecuteOnDelegate() {
    return executeOnDelegate;
  }

  public void setExecuteOnDelegate(boolean executeOnDelegate) {
    this.executeOnDelegate = executeOnDelegate;
  }

  @SchemaIgnore
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @SchemaIgnore
  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }

  /**
   * Instantiates a new Command state.
   *
   * @param name        the name
   * @param commandName the command name
   */
  public CommandState(String name, String commandName) {
    super(name, COMMAND.name());
    this.commandName = commandName;
  }

  /**
   * Instantiates a new Command state.
   *
   * @param name the name
   */
  public CommandState(String name) {
    super(name, COMMAND.name());
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    if (this.getTemplateUuid() != null) {
      return executeLinkedCommand(context);
    }
    return executeCommand(context);
  }

  private ExecutionResponse executeCommand(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();
    String activityId = null;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();

    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);

    String infrastructureMappingId = context.fetchInfraMappingId();
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infrastructureMappingId);

    updateWorkflowExecutionStatsInProgress(context);

    String delegateTaskId;
    try {
      if (instanceElement == null) {
        throw new WingsException("No InstanceElement present in context");
      }

      ServiceInstance serviceInstance = serviceInstanceService.get(appId, envId, instanceElement.getUuid());

      if (serviceInstance == null) {
        throw new WingsException("Unable to find service instance");
      }

      String serviceTemplateId = instanceElement.getServiceTemplateElement().getUuid();
      ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, serviceTemplateId);
      Service service = serviceResourceService.getWithDetails(appId, serviceTemplate.getServiceId());
      Host host =
          hostService.getHostByEnv(serviceInstance.getAppId(), serviceInstance.getEnvId(), serviceInstance.getHostId());

      executionDataBuilder.withServiceId(service.getUuid())
          .withServiceName(service.getName())
          .withTemplateId(serviceTemplateId)
          .withTemplateName(instanceElement.getServiceTemplateElement().getName())
          .withHostId(host.getUuid())
          .withHostName(host.getHostName())
          .withPublicDns(host.getPublicDns())
          .withAppId(appId);

      String actualCommand = commandName;
      try {
        actualCommand = context.renderExpression(commandName);
      } catch (Exception e) {
        logger.error("", e);
      }

      executionDataBuilder.withCommandName(actualCommand);
      ServiceCommand serviceCommand =
          serviceResourceService.getCommandByName(appId, service.getUuid(), envId, actualCommand);
      if (serviceCommand == null || serviceCommand.getCommand() == null) {
        throw new WingsException(format(""
                                         + "Unable to find command %s for service %s",
                                     actualCommand, service.getName()),
            WingsException.USER);
      }
      Command command = serviceCommand.getCommand();

      List<Variable> commandTemplateVariables = command.getTemplateVariables();
      if (serviceCommand.getTemplateUuid()
          != null) { // If linked in service we are overriding command from template to support referenced commands
        command = getCommandFromTemplate(serviceCommand.getTemplateUuid(), serviceCommand.getTemplateVersion());
      }

      command.setGraph(null);

      Application application = appService.get(serviceInstance.getAppId());
      String accountId = application.getAccountId();
      Artifact artifact = null;
      Map<String, Artifact> multiArtifacts = null;
      if (command.isArtifactNeeded()) {
        if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          artifact = findArtifact(service.getUuid(), context);
        } else {
          multiArtifacts = findArtifacts(service.getUuid(), context);
        }
      }

      Map<String, String> serviceVariables = context.getServiceVariables().entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
      Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();

      serviceVariables.replaceAll((name, value) -> context.renderExpression(value));

      if (safeDisplayServiceVariables != null) {
        safeDisplayServiceVariables.replaceAll((name, value) -> context.renderExpression(value));
      }

      DeploymentType deploymentType = serviceResourceService.getDeploymentType(infrastructureMapping, service, null);

      String backupPath = getEvaluatedSettingValue(context, accountId, appId, envId, BACKUP_PATH);
      String runtimePath = getEvaluatedSettingValue(context, accountId, appId, envId, RUNTIME_PATH);
      String stagingPath = getEvaluatedSettingValue(context, accountId, appId, envId, STAGING_PATH);
      String windowsRuntimePath = getEvaluatedSettingValue(context, accountId, appId, envId, WINDOWS_RUNTIME_PATH);

      CommandExecutionContext.Builder commandExecutionContextBuilder =
          aCommandExecutionContext()
              .withAppId(appId)
              .withEnvId(envId)
              .withDeploymentType(deploymentType.name())
              .withBackupPath(backupPath)
              .withRuntimePath(runtimePath)
              .withStagingPath(stagingPath)
              .withWindowsRuntimePath(windowsRuntimePath)
              .withExecutionCredential(workflowStandardParams.getExecutionCredential())
              .withServiceVariables(serviceVariables)
              .withSafeDisplayServiceVariables(safeDisplayServiceVariables)
              .withHost(host)
              .withServiceTemplateId(serviceTemplateId)
              .withAppContainer(service.getAppContainer())
              .withAccountId(accountId)
              .withTimeout(getTimeoutMillis());

      getHostConnectionDetails(context, host, commandExecutionContextBuilder);

      boolean isInExpectedFormat = false;
      if (serviceCommand.getTemplateUuid() != null) {
        Template template = templateService.get(serviceCommand.getTemplateUuid(), serviceCommand.getTemplateVersion());
        if (template != null) {
          SshCommandTemplate sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
          // check if template in new format, If yes, do the new flow else old
          if (isNotEmpty(sshCommandTemplate.getReferencedTemplateList())) {
            isInExpectedFormat = true;
            expandCommand(
                command, serviceCommand.getTemplateUuid(), serviceCommand.getTemplateVersion(), command.getName());
            resolveTemplateVariablesInLinkedCommands(
                command, sshCommandTemplate.getReferencedTemplateList(), commandTemplateVariables);
            command.setTemplateVariables(commandTemplateVariables);
            if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
              renderCommandString(command, context, executionDataBuilder.build(), artifact, true);
            } else {
              renderCommandString(command, context, executionDataBuilder.build(), true);
            }
          } else {
            expandCommand(serviceInstance, command, service.getUuid(), envId);
            executionDataBuilder.withTemplateVariable(
                templateUtils.processTemplateVariables(context, commandTemplateVariables));
            if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
              renderCommandString(command, context, executionDataBuilder.build(), artifact);
            } else {
              renderCommandString(command, context, executionDataBuilder.build());
            }
          }
        }
      } else {
        expandCommand(serviceInstance, command, service.getUuid(), envId);
        executionDataBuilder.withTemplateVariable(
            templateUtils.processTemplateVariables(context, commandTemplateVariables));
        if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          renderCommandString(command, context, executionDataBuilder.build(), artifact);
        } else {
          renderCommandString(command, context, executionDataBuilder.build());
        }
      }

      Map<String, String> flattenedTemplateVariables = new HashMap<>();
      flattenTemplateVariables(command, flattenedTemplateVariables);
      addArtifactTemplateVariablesToContext(flattenedTemplateVariables, multiArtifacts, context);

      if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        if (artifact != null) {
          getArtifactDetails(
              context, executionDataBuilder, service, accountId, artifact, commandExecutionContextBuilder);
        } else if (command.isArtifactNeeded()) {
          throw new WingsException(
              format("Unable to find artifact for service %s", service.getName()), WingsException.USER);
        }
      } else {
        if (isNotEmpty(multiArtifacts)) {
          getMultiArtifactDetails(
              context, executionDataBuilder, service, accountId, multiArtifacts, commandExecutionContextBuilder);
        } else if (command.isArtifactNeeded()) {
          throw new WingsException(
              format("Unable to find artifact for service %s", service.getName()), WingsException.USER);
        }
      }

      List<CommandUnit> flattenCommandUnits;
      if (serviceCommand.getTemplateUuid() != null) {
        if (isInExpectedFormat) {
          flattenCommandUnits =
              getFlattenCommandUnits(appId, envId, service, deploymentType.name(), accountId, command, true);
        } else {
          flattenCommandUnits =
              getFlattenCommandUnits(appId, envId, service, deploymentType.name(), accountId, command, false);
        }
      } else {
        flattenCommandUnits =
            getFlattenCommandUnits(appId, envId, service, deploymentType.name(), accountId, command, false);
      }

      Activity activity;
      if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        activity = activityHelperService.createAndSaveActivity(context, Type.Command, command.getName(),
            command.getCommandUnitType().name(), flattenCommandUnits, artifact);
      } else {
        activity = activityHelperService.createAndSaveActivity(
            context, Type.Command, command.getName(), command.getCommandUnitType().name(), flattenCommandUnits, null);
      }
      activityId = activity.getUuid();
      executionDataBuilder.withActivityId(activityId);

      setPropertiesFromFeatureFlags(accountId, commandExecutionContextBuilder);
      CommandExecutionContext commandExecutionContext =
          commandExecutionContextBuilder.withActivityId(activityId).withDeploymentType(deploymentType.name()).build();

      delegateTaskId = queueDelegateTask(
          activityId, appId, envId, infrastructureMappingId, command, accountId, commandExecutionContext);
      logger.info("DelegateTaskId [{}] sent for activityId [{}]", delegateTaskId, activityId);
    } catch (Exception e) {
      return handleException(context, executionDataBuilder, activityId, appId, e);
    }

    return getExecutionResponse(executionDataBuilder, activityId, delegateTaskId);
  }

  private ExecutionResponse getExecutionResponse(
      CommandStateExecutionData.Builder executionDataBuilder, String activityId, String delegateTaskId) {
    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Collections.singletonList(activityId))
        .stateExecutionData(executionDataBuilder.withDelegateTaskId(delegateTaskId).build())
        .delegateTaskId(delegateTaskId)
        .build();
  }

  private void addArtifactTemplateVariablesToContext(
      Map<String, String> artifactTemplateVariableMap, Map<String, Artifact> multiArtifacts, ExecutionContext context) {
    if (artifactTemplateVariableMap.size() > 0) {
      for (Entry<String, String> entry : artifactTemplateVariableMap.entrySet()) {
        String param = entry.getKey();
        String paramValue = entry.getValue();
        String expression = templateUtils.getExpression(paramValue);
        Artifact evaluatedValue = (Artifact) context.evaluateExpression(expression);
        if (evaluatedValue != null) {
          multiArtifacts.put(param, evaluatedValue);
        }
      }
    }
  }

  private void flattenTemplateVariables(Command command, Map<String, String> artifactTemplateVariables) {
    if (isNotEmpty(command.getTemplateVariables())) {
      for (Variable var : command.getTemplateVariables()) {
        if (var.getType() == VariableType.ARTIFACT) {
          artifactTemplateVariables.put(var.getName(), var.getValue());
        }
      }
      if (isNotEmpty(command.getCommandUnits())) {
        for (CommandUnit cu : command.getCommandUnits()) {
          if (cu instanceof Command) {
            flattenTemplateVariables((Command) cu, artifactTemplateVariables);
          }
        }
      }
    }
  }

  private String queueDelegateTask(String activityId, String appId, String envId, String infrastructureMappingId,
      Command command, String accountId, CommandExecutionContext commandExecutionContext) {
    String delegateTaskId;
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(accountId)
                                    .appId(appId)
                                    .waitId(activityId)
                                    .tags(awsCommandHelper.getAwsConfigTagsFromContext(commandExecutionContext))
                                    .data(TaskData.builder()
                                              .taskType(TaskType.COMMAND.name())
                                              .parameters(new Object[] {command, commandExecutionContext})
                                              .timeout(defaultIfNullTimeout(TimeUnit.MINUTES.toMillis(30)))
                                              .build())
                                    .envId(envId)
                                    .infrastructureMappingId(infrastructureMappingId)
                                    .build();
    delegateTaskId = delegateService.queueTask(delegateTask);
    return delegateTaskId;
  }

  private void getArtifactDetails(ExecutionContext context, CommandStateExecutionData.Builder executionDataBuilder,
      Service service, String accountId, Artifact artifact,
      CommandExecutionContext.Builder commandExecutionContextBuilder) {
    logger.info("Artifact being used: {} for stateExecutionInstanceId: {}", artifact.getUuid(),
        context.getStateExecutionInstanceId());
    commandExecutionContextBuilder.withMetadata(artifact.getMetadata());
    // Observed NPE in alerts
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
    if (artifactStream == null) {
      throw new WingsException(format("Unable to find artifact stream for service %s, artifact %s", service.getName(),
                                   artifact.getArtifactSourceName()),
          WingsException.USER);
    }

    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactStreamId(artifactStream.getUuid());
    if (!ArtifactStreamType.CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        throw new WingsException(
            format("Unable to find Connector/Cloud Provider for artifact stream %s", artifactStream.getSourceName()),
            WingsException.USER);
      }
      artifactStreamAttributes.setServerSetting(settingAttribute);
      artifactStreamAttributes.setArtifactServerEncryptedDataDetails(secretManager.getEncryptionDetails(
          (EncryptableSetting) artifactStreamAttributes.getServerSetting().getValue(), context.getAppId(),
          context.getWorkflowExecutionId()));
      commandExecutionContextBuilder.withArtifactServerEncryptedDataDetails(secretManager.getEncryptionDetails(
          (EncryptableSetting) artifactStreamAttributes.getServerSetting().getValue(), context.getAppId(),
          context.getWorkflowExecutionId()));
    }
    artifactStreamAttributes.setMetadataOnly(artifactStream.isMetadataOnly());
    artifactStreamAttributes.setMetadata(artifact.getMetadata());
    artifactStreamAttributes.setArtifactFileMetadata(artifact.getArtifactFileMetadata());

    if (featureFlagService.isEnabled(FeatureName.COPY_ARTIFACT, accountId)) {
      artifactStreamAttributes.setCopyArtifactEnabled(true);
    }
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    commandExecutionContextBuilder.withArtifactStreamAttributes(artifactStreamAttributes);

    commandExecutionContextBuilder.withArtifactFiles(artifact.getArtifactFiles());
    executionDataBuilder.withArtifactName(artifact.getDisplayName()).withActivityId(artifact.getUuid());
  }

  private void getMultiArtifactDetails(ExecutionContext context, CommandStateExecutionData.Builder executionDataBuilder,
      Service service, String accountId, Map<String, Artifact> map,
      CommandExecutionContext.Builder commandExecutionContextBuilder) {
    commandExecutionContextBuilder.withMultiArtifactMap(map);
    Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap = new HashMap<>();
    Map<String, List<EncryptedDataDetail>> artifactServerEncryptedDataDetailsMap = new HashMap<>();
    if (isNotEmpty(map)) {
      for (Entry<String, Artifact> entry : map.entrySet()) {
        Artifact artifact = entry.getValue();
        logger.info("Artifact being used: {} for stateExecutionInstanceId: {}", artifact.getUuid(),
            context.getStateExecutionInstanceId());
        // Observed NPE in alerts
        ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
        if (artifactStream == null) {
          throw new WingsException(format("Unable to find artifact stream for service %s, artifact %s",
                                       service.getName(), artifact.getArtifactSourceName()),
              WingsException.USER);
        }

        ArtifactStreamAttributes artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
        artifactStreamAttributes.setArtifactStreamId(artifactStream.getUuid());
        if (!ArtifactStreamType.CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
          SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
          if (settingAttribute == null) {
            throw new WingsException(format("Unable to find Connector/Cloud Provider for artifact stream %s",
                                         artifactStream.getSourceName()),
                WingsException.USER);
          }
          artifactStreamAttributes.setServerSetting(settingAttribute);
          List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
              (EncryptableSetting) artifactStreamAttributes.getServerSetting().getValue(), context.getAppId(),
              context.getWorkflowExecutionId());
          artifactStreamAttributes.setArtifactServerEncryptedDataDetails(encryptedDataDetails);
          artifactServerEncryptedDataDetailsMap.put(artifact.getUuid(), encryptedDataDetails);
        }
        artifactStreamAttributes.setMetadataOnly(artifactStream.isMetadataOnly());
        artifactStreamAttributes.setMetadata(artifact.getMetadata());
        artifactStreamAttributes.setArtifactFileMetadata(artifact.getArtifactFileMetadata());

        if (featureFlagService.isEnabled(FeatureName.COPY_ARTIFACT, accountId)) {
          artifactStreamAttributes.setCopyArtifactEnabled(true);
        }
        artifactStreamAttributes.setArtifactType(service.getArtifactType());
        artifactStreamAttributesMap.put(artifact.getUuid(), artifactStreamAttributes);

        artifact.setArtifactFiles(artifactService.fetchArtifactFiles(artifact.getUuid()));
      }
      commandExecutionContextBuilder.withArtifactStreamAttributesMap(artifactStreamAttributesMap);
      commandExecutionContextBuilder.withArtifactServerEncryptedDataDetailsMap(artifactServerEncryptedDataDetailsMap);
      addArtifactFileNameToContext(map, artifactStreamAttributesMap, commandExecutionContextBuilder);
    }
  }

  private void addArtifactFileNameToContext(Map<String, Artifact> multiArtifactMap,
      Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap,
      CommandExecutionContext.Builder commandExecutionContextBuilder) {
    artifactFileName = resolveArtifactFileName(multiArtifactMap, artifactStreamAttributesMap);
    // add $ARTIFACT_FILE_NAME to context
    if (isNotEmpty(artifactFileName)) {
      commandExecutionContextBuilder.withArtifactFileName(artifactFileName);
    }
  }

  private String resolveArtifactFileName(
      Map<String, Artifact> multiArtifactMap, Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap) {
    String artifactFileName = null;
    if (multiArtifactMap.size() == 1) {
      String artifactVariableName = multiArtifactMap.keySet().stream().findFirst().isPresent()
          ? multiArtifactMap.keySet().stream().findFirst().get()
          : null;
      Artifact artifact = multiArtifactMap.get(artifactVariableName);
      if (artifact != null) {
        ArtifactStreamAttributes artifactStreamAttributes = artifactStreamAttributesMap.get(artifact.getUuid());
        if (artifactStreamAttributes == null) {
          throw new InvalidRequestException(
              format("ArtifactStreamAttributes not found for artifact: %s", artifactVariableName));
        }
        if (isNotEmpty(artifact.getArtifactFiles())) {
          String name = artifact.getArtifactFiles().get(0).getName();
          if (isNotEmpty(name)) {
            artifactFileName = name;
          }
        } else if (artifactStreamAttributes.getMetadata() != null) {
          String value = artifactStreamAttributes.getMetadata().get(ArtifactMetadataKeys.artifactFileName);
          if (isNotEmpty(value)) {
            artifactFileName = value;
          }
        }
      }
    }
    return artifactFileName;
  }

  private void getHostConnectionDetails(
      ExecutionContext context, Host host, CommandExecutionContext.Builder commandExecutionContextBuilder) {
    if (isNotEmpty(host.getHostConnAttr())) {
      SettingAttribute hostConnectionAttribute = settingsService.get(host.getHostConnAttr());
      commandExecutionContextBuilder.withHostConnectionAttributes(hostConnectionAttribute);
      commandExecutionContextBuilder.withHostConnectionCredentials(
          secretManager.getEncryptionDetails((EncryptableSetting) hostConnectionAttribute.getValue(),
              context.getAppId(), context.getWorkflowExecutionId()));
    }
    if (isNotEmpty(host.getBastionConnAttr())) {
      SettingAttribute bastionConnectionAttribute = settingsService.get(host.getBastionConnAttr());
      commandExecutionContextBuilder.withBastionConnectionAttributes(bastionConnectionAttribute);
      commandExecutionContextBuilder.withBastionConnectionCredentials(
          secretManager.getEncryptionDetails((EncryptableSetting) bastionConnectionAttribute.getValue(),
              context.getAppId(), context.getWorkflowExecutionId()));
    }
    if (isNotEmpty(host.getWinrmConnAttr())) {
      WinRmConnectionAttributes winrmConnectionAttribute =
          (WinRmConnectionAttributes) settingsService.get(host.getWinrmConnAttr()).getValue();
      commandExecutionContextBuilder.withWinRmConnectionAttributes(winrmConnectionAttribute);
      commandExecutionContextBuilder.withWinrmConnectionEncryptedDataDetails(secretManager.getEncryptionDetails(
          winrmConnectionAttribute, context.getAppId(), context.getWorkflowExecutionId()));
    }
  }

  private ExecutionResponse executeLinkedCommand(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();
    String activityId = null;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();
    Host host = null;
    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);
    ServiceTemplate serviceTemplate = null;
    if (instanceElement != null) {
      String serviceTemplateId = instanceElement.getServiceTemplateElement().getUuid();
      serviceTemplate = serviceTemplateService.get(appId, serviceTemplateId);
    }

    String infrastructureMappingId = context.fetchInfraMappingId();
    InfrastructureMapping infrastructureMapping = null;
    Service service = null;
    if (infrastructureMappingId != null) {
      infrastructureMapping = infrastructureMappingService.get(appId, infrastructureMappingId);
      if (infrastructureMapping != null) {
        service = serviceResourceService.getWithDetails(appId, infrastructureMapping.getServiceId());
        if (instanceElement == null) {
          serviceTemplate = serviceTemplateHelper.fetchServiceTemplate(infrastructureMapping);
        }
      }
    }

    DeploymentType deploymentType = null;
    if (executeOnDelegate) {
      deploymentType = DeploymentType.SSH;
      executionDataBuilder.withServiceId(service != null ? service.getUuid() : null)
          .withServiceName(service != null ? service.getName() : null)
          .withTemplateId(serviceTemplate != null ? serviceTemplate.getUuid() : null)
          .withTemplateName(instanceElement != null ? instanceElement.getServiceTemplateElement().getName() : null)
          .withAppId(appId);
    } else {
      if (this.getHost() == null) {
        throw new WingsException("Host cannot be empty");
      } else { // host can contain either ${instance.hostName} or some hostname/ip
        // take user provided value for host
        if (connectionType == null || connectionType == ConnectionType.SSH) {
          deploymentType = DeploymentType.SSH;
          if (isEmpty(sshKeyRef)) {
            throw new WingsException("SSH Connection Attribute not provided in Command Step", USER);
          }
          SettingAttribute keySettingAttribute = settingsService.get(sshKeyRef);
          if (keySettingAttribute == null) {
            throw new WingsException("SSH Connection Attribute provided in Command Step not found", USER);
          }
          String hostName = context.renderExpression(this.getHost());
          host = Host.Builder.aHost()
                     .withHostName(hostName)
                     .withPublicDns(hostName)
                     .withAppId(appId)
                     .withHostConnAttr(sshKeyRef)
                     .build();
          executionDataBuilder.withServiceId(service != null ? service.getUuid() : null)
              .withTemplateId(serviceTemplate != null ? serviceTemplate.getUuid() : null)
              .withTemplateName(instanceElement != null ? instanceElement.getServiceTemplateElement().getName() : null)
              .withHostName(hostName)
              .withPublicDns(hostName)
              .withAppId(appId);
        } else if (connectionType == ConnectionType.WINRM) {
          deploymentType = DeploymentType.WINRM;
          if (isEmpty(connectionAttributes)) {
            throw new WingsException("WinRM Connection Attribute not provided in Command Step", USER);
          }
          WinRmConnectionAttributes winRmConnectionAttributes =
              (WinRmConnectionAttributes) settingsService.get(connectionAttributes).getValue();
          if (winRmConnectionAttributes == null) {
            throw new WingsException("WinRM Connection Attribute provided in Command Step not found", USER);
          }
          String hostName = context.renderExpression(this.getHost());
          host = Host.Builder.aHost()
                     .withHostName(hostName)
                     .withPublicDns(hostName)
                     .withAppId(appId)
                     .withWinrmConnAttr(connectionAttributes)
                     .build();
          executionDataBuilder.withServiceId(service != null ? service.getUuid() : null)
              .withTemplateId(serviceTemplate != null ? serviceTemplate.getUuid() : null)
              .withTemplateName(instanceElement != null ? instanceElement.getServiceTemplateElement().getName() : null)
              .withHostName(hostName)
              .withPublicDns(hostName)
              .withAppId(appId);
        }
      }
    }
    updateWorkflowExecutionStatsInProgress(context);

    String delegateTaskId;
    try {
      Command command = getCommandFromTemplate(this.getTemplateUuid(), this.getTemplateVersion());
      executionDataBuilder.withCommandName(command.getName());

      Application application = appService.get(appId);
      String accountId = application.getAccountId();
      Artifact artifact = null;
      Map<String, Artifact> multiArtifacts = null;

      if (command.isArtifactNeeded()) {
        if (service == null) {
          throw new WingsException("Linked Command needs artifact but service is not found", USER);
        }
        if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          artifact = findArtifact(service.getUuid(), context);
        } else {
          multiArtifacts = findArtifacts(service.getUuid(), context);
        }
      }

      Map<String, String> serviceVariables = context.getServiceVariables().entrySet().stream().collect(
          Collectors.toMap(Entry::getKey, e -> e.getValue().toString()));
      Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();

      if (serviceVariables != null) {
        serviceVariables.replaceAll((name, value) -> context.renderExpression(value));
      }

      if (safeDisplayServiceVariables != null) {
        safeDisplayServiceVariables.replaceAll((name, value) -> context.renderExpression(value));
      }

      String backupPath = getEvaluatedSettingValue(context, accountId, appId, envId, BACKUP_PATH);
      String runtimePath = getEvaluatedSettingValue(context, accountId, appId, envId, RUNTIME_PATH);
      String stagingPath = getEvaluatedSettingValue(context, accountId, appId, envId, STAGING_PATH);
      String windowsRuntimePath = getEvaluatedSettingValue(context, accountId, appId, envId, WINDOWS_RUNTIME_PATH);

      CommandExecutionContext.Builder commandExecutionContextBuilder =
          aCommandExecutionContext()
              .withAppId(appId)
              .withEnvId(envId)
              .withBackupPath(backupPath)
              .withRuntimePath(runtimePath)
              .withStagingPath(stagingPath)
              .withWindowsRuntimePath(windowsRuntimePath)
              .withExecutionCredential(workflowStandardParams.getExecutionCredential())
              .withServiceVariables(serviceVariables)
              .withSafeDisplayServiceVariables(safeDisplayServiceVariables)
              .withServiceTemplateId(serviceTemplate != null ? serviceTemplate.getUuid() : null)
              .withAppContainer(service != null ? service.getAppContainer() : null)
              .withHost(host)
              .withAccountId(accountId)
              .withTimeout(getTimeoutMillis())
              .withExecuteOnDelegate(executeOnDelegate)
              .withDeploymentType(deploymentType != null ? deploymentType.name() : null);

      if (host != null) {
        getHostConnectionDetails(context, host, commandExecutionContextBuilder);
      }

      String templateId = this.getTemplateUuid();
      String templateVersion = this.getTemplateVersion();
      expandCommand(command, templateId, templateVersion, command.getName());
      Template template = templateService.get(templateId, templateVersion);
      if (template != null) {
        SshCommandTemplate sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
        resolveTemplateVariablesInLinkedCommands(
            command, sshCommandTemplate.getReferencedTemplateList(), this.getTemplateVariables());
      }

      if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        renderCommandString(command, context, executionDataBuilder.build(), artifact, true);
      } else {
        renderCommandString(command, context, executionDataBuilder.build(), true);
      }

      Map<String, String> flattenedTemplateVariables = new HashMap<>();
      flattenTemplateVariables(command, flattenedTemplateVariables);
      addArtifactTemplateVariablesToContext(flattenedTemplateVariables, multiArtifacts, context);

      if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        if (artifact != null) {
          getArtifactDetails(
              context, executionDataBuilder, service, accountId, artifact, commandExecutionContextBuilder);
        } else if (command.isArtifactNeeded()) {
          if (service != null) {
            throw new WingsException(
                format("Unable to find artifact for service %s", service.getName()), WingsException.USER);
          }
          throw new WingsException("Command needs artifact. However, service not found.", USER);
        }
      } else {
        if (isNotEmpty(multiArtifacts)) {
          getMultiArtifactDetails(
              context, executionDataBuilder, service, accountId, multiArtifacts, commandExecutionContextBuilder);
        } else if (command.isArtifactNeeded()) {
          if (service != null) {
            throw new WingsException(
                format("Unable to find artifact for service %s", service.getName()), WingsException.USER);
          }
          throw new WingsException("Command needs artifact. However, service not found.", USER);
        }
      }

      List<CommandUnit> flattenCommandUnits =
          getFlattenCommandUnits(appId, envId, service, deploymentType.name(), accountId, command, true);
      Activity activity = activityHelperService.createAndSaveActivity(
          context, Type.Command, command.getName(), command.getCommandUnitType().name(), flattenCommandUnits, artifact);
      activityId = activity.getUuid();

      executionDataBuilder.withActivityId(activityId);
      executionDataBuilder.withTemplateVariable(
          templateUtils.processTemplateVariables(context, command.getTemplateVariables()));
      setPropertiesFromFeatureFlags(accountId, commandExecutionContextBuilder);
      CommandExecutionContext commandExecutionContext =
          commandExecutionContextBuilder.withActivityId(activityId).withDeploymentType(deploymentType.name()).build();

      delegateTaskId = queueDelegateTask(
          activityId, appId, envId, infrastructureMappingId, command, accountId, commandExecutionContext);
      logger.info("DelegateTaskId [{}] sent for activityId [{}]", delegateTaskId, activityId);
    } catch (Exception e) {
      return handleException(context, executionDataBuilder, activityId, appId, e);
    }

    return getExecutionResponse(executionDataBuilder, activityId, delegateTaskId);
  }

  private void setPropertiesFromFeatureFlags(
      String accountId, CommandExecutionContext.Builder commandExecutionContextBuilder) {
    if (featureFlagService.isEnabled(FeatureName.INLINE_SSH_COMMAND, accountId)) {
      commandExecutionContextBuilder.withInlineSshCommand(true);
    }
    commandExecutionContextBuilder.withMultiArtifact(
        featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId));
  }

  private ExecutionResponse handleException(ExecutionContext context,
      CommandStateExecutionData.Builder executionDataBuilder, String activityId, String appId, Exception e) {
    if (e instanceof WingsException) {
      logger.warn("Exception in command execution", e);
    } else {
      // unhandled exception
      logger.error("Exception in command execution", e);
    }
    handleCommandException(context, activityId, appId);
    updateWorkflowExecutionStats(ExecutionStatus.FAILED, context);
    return ExecutionResponse.builder()
        .executionStatus(ExecutionStatus.FAILED)
        .stateExecutionData(executionDataBuilder.build())
        .errorMessage(ExceptionUtils.getMessage(e))
        .build();
  }

  private Command getCommandFromTemplate(String templateId, String version) {
    Template template = templateService.get(templateId, version);
    Command commandEntity = aCommand().build();
    if (this.getTemplateUuid() != null) {
      commandEntity.setTemplateVariables(getTemplateVariables());
    } else {
      commandEntity.setTemplateVariables(template.getVariables());
    }
    if (template != null) {
      SshCommandTemplate sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
      commandEntity.setName(template.getName());
      commandEntity.setCommandType(sshCommandTemplate.getCommandType());
      commandEntity.setCommandUnits(sshCommandTemplate.getCommandUnits());
      for (CommandUnit commandUnit : commandEntity.getCommandUnits()) {
        if (commandUnit instanceof Command) {
          if (((Command) commandUnit).getTemplateReference() != null) {
            template = templateService.get(((Command) commandUnit).getTemplateReference().getTemplateUuid(),
                String.valueOf(((Command) commandUnit).getTemplateReference().getTemplateVersion()));
            if (template != null) {
              ((Command) commandUnit)
                  .setCommandUnits(((SshCommandTemplate) template.getTemplateObject()).getCommandUnits());
            }
          }
        }
      }
    }
    return commandEntity;
  }

  void renderCommandString(Command command, ExecutionContext context,
      CommandStateExecutionData commandStateExecutionData, Artifact artifact) {
    renderCommandString(command, context, commandStateExecutionData, artifact, false);
  }

  void renderCommandString(Command command, ExecutionContext context,
      CommandStateExecutionData commandStateExecutionData, Artifact artifact, boolean linkedFromTemplateLibrary) {
    for (CommandUnit commandUnit : command.getCommandUnits()) {
      if (CommandUnitType.COMMAND == commandUnit.getCommandUnitType()) {
        Command commandCommandUnit = (Command) commandUnit;
        commandStateExecutionData.setTemplateVariable(
            templateUtils.processTemplateVariables(context, commandCommandUnit.getTemplateVariables()));
        renderCommandString(
            (Command) commandUnit, context, commandStateExecutionData, artifact, linkedFromTemplateLibrary);
        continue;
      }

      if (linkedFromTemplateLibrary) {
        commandStateExecutionData.setTemplateVariable(
            templateUtils.processTemplateVariables(context, command.getTemplateVariables()));
        commandUnit.setVariables(command.getTemplateVariables());
      }

      if (commandUnit instanceof ScpCommandUnit) {
        ScpCommandUnit scpCommandUnit = (ScpCommandUnit) commandUnit;
        if (isNotEmpty(scpCommandUnit.getDestinationDirectoryPath())) {
          scpCommandUnit.setDestinationDirectoryPath(
              context.renderExpression(scpCommandUnit.getDestinationDirectoryPath(),
                  StateExecutionContext.builder()
                      .stateExecutionData(commandStateExecutionData)
                      .artifact(artifact)
                      .build()));
        }
      }

      if (commandUnit instanceof CopyConfigCommandUnit) {
        CopyConfigCommandUnit copyConfigCommandUnit = (CopyConfigCommandUnit) commandUnit;
        if (isNotEmpty(copyConfigCommandUnit.getDestinationParentPath())) {
          copyConfigCommandUnit.setDestinationParentPath(
              context.renderExpression(copyConfigCommandUnit.getDestinationParentPath(),
                  StateExecutionContext.builder()
                      .stateExecutionData(commandStateExecutionData)
                      .artifact(artifact)
                      .build()));
        }
      }

      if (!(commandUnit instanceof ExecCommandUnit)) {
        continue;
      }
      ExecCommandUnit execCommandUnit = (ExecCommandUnit) commandUnit;
      if (isNotEmpty(execCommandUnit.getCommandPath())) {
        execCommandUnit.setCommandPath(context.renderExpression(execCommandUnit.getCommandPath(),
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).artifact(artifact).build()));
      }
      if (isNotEmpty(execCommandUnit.getCommandString())) {
        execCommandUnit.setCommandString(context.renderExpression(execCommandUnit.getCommandString(),
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).artifact(artifact).build()));
      }
      if (isNotEmpty(execCommandUnit.getTailPatterns())) {
        renderTailFilePattern(context, commandStateExecutionData, artifact, execCommandUnit);
      }
    }
  }

  void renderCommandString(
      Command command, ExecutionContext context, CommandStateExecutionData commandStateExecutionData) {
    renderCommandString(command, context, commandStateExecutionData, false);
  }

  void renderCommandString(Command command, ExecutionContext context,
      CommandStateExecutionData commandStateExecutionData, boolean linkedFromTemplateLibrary) {
    for (CommandUnit commandUnit : command.getCommandUnits()) {
      if (CommandUnitType.COMMAND == commandUnit.getCommandUnitType()) {
        Command commandCommandUnit = (Command) commandUnit;
        commandStateExecutionData.setTemplateVariable(
            templateUtils.processTemplateVariables(context, commandCommandUnit.getTemplateVariables()));
        renderCommandString((Command) commandUnit, context, commandStateExecutionData, linkedFromTemplateLibrary);
        continue;
      }

      if (linkedFromTemplateLibrary) {
        commandStateExecutionData.setTemplateVariable(
            templateUtils.processTemplateVariables(context, command.getTemplateVariables()));
        commandUnit.setVariables(command.getTemplateVariables());
      }

      if (commandUnit instanceof ScpCommandUnit) {
        ScpCommandUnit scpCommandUnit = (ScpCommandUnit) commandUnit;
        if (isNotEmpty(scpCommandUnit.getDestinationDirectoryPath())) {
          StateExecutionContextBuilder stateExecutionContextBuilder =
              StateExecutionContext.builder().stateExecutionData(commandStateExecutionData);
          if (artifactFileName != null) {
            stateExecutionContextBuilder.artifactFileName(artifactFileName);
          }
          scpCommandUnit.setDestinationDirectoryPath(context.renderExpression(
              scpCommandUnit.getDestinationDirectoryPath(), stateExecutionContextBuilder.build()));
        }
      }

      if (commandUnit instanceof CopyConfigCommandUnit) {
        CopyConfigCommandUnit copyConfigCommandUnit = (CopyConfigCommandUnit) commandUnit;
        if (isNotEmpty(copyConfigCommandUnit.getDestinationParentPath())) {
          StateExecutionContextBuilder stateExecutionContextBuilder =
              StateExecutionContext.builder().stateExecutionData(commandStateExecutionData);
          if (artifactFileName != null) {
            stateExecutionContextBuilder.artifactFileName(artifactFileName);
          }
          copyConfigCommandUnit.setDestinationParentPath(context.renderExpression(
              copyConfigCommandUnit.getDestinationParentPath(), stateExecutionContextBuilder.build()));
        }
      }

      if (!(commandUnit instanceof ExecCommandUnit)) {
        continue;
      }
      ExecCommandUnit execCommandUnit = (ExecCommandUnit) commandUnit;
      if (isNotEmpty(execCommandUnit.getCommandPath())) {
        StateExecutionContextBuilder stateExecutionContextBuilder =
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData);
        if (artifactFileName != null) {
          stateExecutionContextBuilder.artifactFileName(artifactFileName);
        }
        execCommandUnit.setCommandPath(
            context.renderExpression(execCommandUnit.getCommandPath(), stateExecutionContextBuilder.build()));
      }
      if (isNotEmpty(execCommandUnit.getCommandString())) {
        StateExecutionContextBuilder stateExecutionContextBuilder =
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData);
        execCommandUnit.setCommandString(
            context.renderExpression(execCommandUnit.getCommandString(), stateExecutionContextBuilder.build()));
      }
      if (isNotEmpty(execCommandUnit.getTailPatterns())) {
        renderTailFilePattern(context, commandStateExecutionData, execCommandUnit);
      }
    }
  }

  void renderTailFilePattern(
      ExecutionContext context, CommandStateExecutionData commandStateExecutionData, ExecCommandUnit execCommandUnit) {
    List<TailFilePatternEntry> filePatternEntries = execCommandUnit.getTailPatterns();
    for (TailFilePatternEntry filePatternEntry : filePatternEntries) {
      if (isNotEmpty(filePatternEntry.getFilePath())) {
        filePatternEntry.setFilePath(context.renderExpression(filePatternEntry.getFilePath(),
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build()));
      }
      if (isNotEmpty(filePatternEntry.getPattern())) {
        filePatternEntry.setPattern(context.renderExpression(filePatternEntry.getPattern(),
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build()));
      }
    }
    execCommandUnit.setTailPatterns(filePatternEntries);
  }

  void renderTailFilePattern(ExecutionContext context, CommandStateExecutionData commandStateExecutionData,
      Artifact artifact, ExecCommandUnit execCommandUnit) {
    List<TailFilePatternEntry> filePatternEntries = execCommandUnit.getTailPatterns();
    for (TailFilePatternEntry filePatternEntry : filePatternEntries) {
      if (isNotEmpty(filePatternEntry.getFilePath())) {
        filePatternEntry.setFilePath(context.renderExpression(filePatternEntry.getFilePath(),
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).artifact(artifact).build()));
      }
      if (isNotEmpty(filePatternEntry.getPattern())) {
        filePatternEntry.setPattern(context.renderExpression(filePatternEntry.getPattern(),
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).artifact(artifact).build()));
      }
    }
    execCommandUnit.setTailPatterns(filePatternEntries);
  }

  private List<CommandUnit> getFlattenCommandUnits(String appId, String envId, Service service, String deploymentType,
      String accountId, Command templateCommand, boolean fromTemplate) {
    List<CommandUnit> flattenCommandUnitList;
    if (fromTemplate) {
      flattenCommandUnitList = getFlattenCommandUnitList(templateCommand);
    } else {
      flattenCommandUnitList =
          serviceResourceService.getFlattenCommandUnitList(appId, service.getUuid(), envId, commandName);
    }

    // if (!executeOnDelegate) {
    if (DeploymentType.SSH.name().equals(deploymentType)) {
      if (getScriptType(flattenCommandUnitList) == ScriptType.POWERSHELL) {
        flattenCommandUnitList.add(0, new InitPowerShellCommandUnit());
        flattenCommandUnitList.add(new CleanupPowerShellCommandUnit());
      } else {
        if (featureFlagService.isEnabled(FeatureName.INLINE_SSH_COMMAND, accountId)) {
          flattenCommandUnitList.add(0, new InitSshCommandUnitV2());
        } else {
          flattenCommandUnitList.add(0, new InitSshCommandUnit());
        }
        flattenCommandUnitList.add(new CleanupSshCommandUnit());
      }
    } else if (DeploymentType.WINRM.name().equals(deploymentType)) {
      if (getScriptType(flattenCommandUnitList) == ScriptType.POWERSHELL) {
        flattenCommandUnitList.add(0, new InitPowerShellCommandUnit());
        flattenCommandUnitList.add(new CleanupPowerShellCommandUnit());
      }
    }
    // }
    return flattenCommandUnitList;
  }

  private List<CommandUnit> getFlattenCommandUnitList(Command command) {
    return command.getCommandUnits()
        .stream()
        .flatMap(commandUnit -> {
          if (CommandUnitType.COMMAND == commandUnit.getCommandUnitType()) {
            return getFlattenCommandUnitList((Command) commandUnit).stream();
          } else {
            return Stream.of(commandUnit);
          }
        })
        .collect(toList());
  }

  private Artifact findArtifact(String serviceId, ExecutionContext context) {
    if (isRollback()) {
      final Artifact previousArtifact = serviceResourceService.findPreviousArtifact(
          context.getAppId(), context.getWorkflowExecutionId(), context.getContextElement(ContextElementType.INSTANCE));
      if (previousArtifact != null) {
        return previousArtifact;
      }
    }
    return ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
  }

  private Map<String, Artifact> findArtifacts(String serviceId, ExecutionContext context) {
    // NOTE: This function also takes care of rollback.
    return ((DeploymentExecutionContext) context).getArtifactsForService(serviceId);
  }

  private ScriptType getScriptType(List<CommandUnit> commandUnits) {
    if (commandUnits.stream().anyMatch(unit
            -> (unit.getCommandUnitType() == CommandUnitType.EXEC
                   || unit.getCommandUnitType() == CommandUnitType.DOWNLOAD_ARTIFACT)
                && ((ExecCommandUnit) unit).getScriptType() == ScriptType.POWERSHELL)) {
      return ScriptType.POWERSHELL;
    } else {
      return ScriptType.BASH;
    }
  }

  private void handleCommandException(ExecutionContext context, String activityId, String appId) {
    if (activityId != null) {
      activityHelperService.updateStatus(activityId, appId, ExecutionStatus.FAILED);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    if (response.size() != 1) {
      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Unexpected number of response data items")
          .build();
    }

    ResponseData notifyResponseData = response.values().iterator().next();

    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage())
          .build();
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();

    CommandExecutionResult commandExecutionResult = (CommandExecutionResult) notifyResponseData;
    String activityId = response.keySet().iterator().next();

    if (commandExecutionResult.getStatus() != SUCCESS && isNotEmpty(commandExecutionResult.getErrorMessage())) {
      handleCommandException(context, activityId, appId);
    }

    activityHelperService.updateStatus(activityId, appId,
        commandExecutionResult.getStatus() == SUCCESS ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED);

    ExecutionStatus executionStatus =
        commandExecutionResult.getStatus() == SUCCESS ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    updateWorkflowExecutionStats(executionStatus, context);

    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();
    commandStateExecutionData.setStatus(executionStatus);
    on(commandStateExecutionData).set("activityService", activityService);
    commandStateExecutionData.setCountsByStatuses(
        (CountsByStatuses) commandStateExecutionData.getExecutionSummary().get("breakdown").getValue());
    commandStateExecutionData.setDelegateMetaInfo(commandExecutionResult.getDelegateMetaInfo());

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(commandExecutionResult.getErrorMessage())
        .stateExecutionData(commandStateExecutionData)
        .build();
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private void updateWorkflowExecutionStats(ExecutionStatus executionStatus, ExecutionContext context) {
    Optional<ContextElement> simpleWorkflowParamOpt =
        context.getContextElementList(ContextElementType.PARAM)
            .stream()
            .filter(contextElement -> contextElement instanceof SimpleWorkflowParam)
            .findFirst();
    if (simpleWorkflowParamOpt.isPresent()) {
      String appId = getAppId(context);
      if (executionStatus == ExecutionStatus.FAILED) {
        workflowExecutionService.incrementFailed(appId, context.getWorkflowExecutionId(), 1);
      } else {
        workflowExecutionService.incrementSuccess(appId, context.getWorkflowExecutionId(), 1);
      }
    }
  }

  private String getAppId(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    return workflowStandardParams.getAppId();
  }

  private void updateWorkflowExecutionStatsInProgress(ExecutionContext context) {
    Optional<ContextElement> simpleWorkflowParamOpt =
        context.getContextElementList(ContextElementType.PARAM)
            .stream()
            .filter(contextElement -> contextElement instanceof SimpleWorkflowParam)
            .findFirst();
    if (simpleWorkflowParamOpt.isPresent()) {
      String appId = getAppId(context);
      workflowExecutionService.incrementInProgressCount(appId, context.getWorkflowExecutionId(), 1);
    }
  }

  private String getEvaluatedSettingValue(
      ExecutionContext context, String accountId, String appId, String envId, String variable) {
    SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, envId, variable);

    if (settingAttribute == null) {
      return "";
    }

    StringValue stringValue = (StringValue) settingAttribute.getValue();
    String settingValue = stringValue.getValue();
    try {
      settingValue = context.renderExpression(settingValue);
    } catch (Exception e) {
      // ignore
    }
    return settingValue;
  }

  private void expandCommand(ServiceInstance serviceInstance, Command command, String serviceId, String envId) {
    if (isNotEmpty(command.getReferenceId())) {
      Command referredCommand = Optional
                                    .ofNullable(serviceResourceService.getCommandByName(
                                        serviceInstance.getAppId(), serviceId, envId, command.getReferenceId()))
                                    .orElse(aServiceCommand().build())
                                    .getCommand();
      if (referredCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST, USER);
      }
      command.setCommandUnits(referredCommand.getCommandUnits());
      command.setTemplateVariables(referredCommand.getTemplateVariables());
    }

    for (CommandUnit commandUnit : command.getCommandUnits()) {
      if (CommandUnitType.COMMAND == commandUnit.getCommandUnitType()) {
        expandCommand(serviceInstance, (Command) commandUnit, serviceId, envId);
      }
    }
  }

  private void expandCommand(Command command, String templateId, String templateVersion, String parentName) {
    Command referredCommand = Optional.ofNullable(getCommandFromTemplate(templateId, templateVersion))
                                  .orElse(aServiceCommand().build().getCommand());

    if (referredCommand == null) {
      throw new WingsException(COMMAND_DOES_NOT_EXIST, USER);
    }
    command.setCommandUnits(referredCommand.getCommandUnits());
    for (CommandUnit commandUnit : command.getCommandUnits()) {
      if (CommandUnitType.COMMAND == commandUnit.getCommandUnitType()) {
        if (((Command) commandUnit).getTemplateReference() == null) {
          throw new WingsException("No command units found in command " + commandUnit.getName(), USER);
        }
        expandCommand((Command) commandUnit, ((Command) commandUnit).getTemplateReference().getTemplateUuid(),
            String.valueOf(((Command) commandUnit).getTemplateReference().getTemplateVersion()),
            parentName + "/" + commandUnit.getName());
      } else {
        commandUnit.setName(parentName + "/" + commandUnit.getName());
      }
    }
  }

  private void resolveTemplateVariablesInLinkedCommands(
      Command command, List<ReferencedTemplate> referencedTemplateList, List<Variable> globalVariables) {
    for (int i = 0; i < command.getCommandUnits().size(); i++) {
      CommandUnit commandUnit = command.getCommandUnits().get(i);
      if (CommandUnitType.COMMAND == commandUnit.getCommandUnitType()) {
        List<Variable> variables = ((Command) commandUnit).getTemplateVariables();
        if (isNotEmpty(referencedTemplateList)) {
          ReferencedTemplate referencedTemplate = referencedTemplateList.get(i);
          if (referencedTemplate != null) {
            Map<String, Variable> variableMapping = referencedTemplate.getVariableMapping();
            if (isNotEmpty(variables) && isNotEmpty(variableMapping)) {
              for (Variable variable : variables) {
                if (variableMapping.containsKey(variable.getName())) {
                  Variable mappedVariable = variableMapping.get(variable.getName());
                  variable.setValue(getTopLevelTemplateVariableValue(globalVariables, mappedVariable.getName()));
                }
              }
            }
          }
        }
      }
    }
  }

  private String getTopLevelTemplateVariableValue(List<Variable> variables, String lookupVariable) {
    if (isNotEmpty(variables)) {
      for (Variable variable : variables) {
        if (variable.getName().equals(lookupVariable)) {
          return variable.getValue();
        }
      }
    }
    return null;
  }
  @Override
  @SchemaIgnore
  public List<EntityType> getRequiredExecutionArgumentTypes() {
    return Lists.newArrayList(EntityType.SERVICE, EntityType.INSTANCE);
  }

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    if (executeOnDelegate) {
      return null;
    }
    if (host != null) {
      return asList(host);
    }
    return asList("${instance}");
  }

  /**
   * Gets executor service.
   *
   * @return the executor service
   */
  @SchemaIgnore
  public ExecutorService getExecutorService() {
    return executorService;
  }

  /**
   * Sets executor service.
   *
   * @param executorService the executor service
   */
  @SchemaIgnore
  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String commandName;
    private String name;

    private Builder() {}

    /**
     * A command state builder.
     *
     * @return the builder
     */
    public static Builder aCommandState() {
      return new Builder();
    }

    /**
     * With command name builder.
     *
     * @param commandName the command name
     * @return the builder
     */
    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * Build command state.
     *
     * @return the command state
     */
    public CommandState build() {
      CommandState commandState = new CommandState(name);
      commandState.setCommandName(commandName);
      return commandState;
    }
  }
}
