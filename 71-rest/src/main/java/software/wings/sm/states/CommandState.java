package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.eraro.ErrorCode.COMMAND_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static org.joor.Reflect.on;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.beans.template.TemplateHelper.convertToVariableMap;
import static software.wings.common.Constants.WINDOWS_RUNTIME_PATH;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.COMMAND;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.waiter.ErrorNotifyResponseData;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.DelegateTask;
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
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.CleanupPowerShellCommandUnit;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitPowerShellCommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.beans.command.InitSshCommandUnitV2;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.command.TailFilePatternEntry;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.Expand;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
public class CommandState extends State {
  private static final Logger logger = LoggerFactory.getLogger(CommandState.class);

  public static final String RUNTIME_PATH = "RUNTIME_PATH";
  public static final String BACKUP_PATH = "BACKUP_PATH";
  public static final String STAGING_PATH = "STAGING_PATH";

  @Inject @Transient @SchemaIgnore private transient ExecutorService executorService;
  @Inject @Transient private transient ActivityHelperService activityHelperService;
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient AppService appService;
  @Inject @Transient private transient ArtifactService artifactService;
  @Inject @Transient private transient ArtifactStreamService artifactStreamService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient EnvironmentService environmentService;
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

  @Inject @Transient private transient WingsPersistence wingsPersistence;

  @Attributes(title = "Command") @Expand(dataProvider = CommandStateEnumDataProvider.class) private String commandName;

  /**
   * Instantiates a new Command state.
   *
   * @param name        the name
   * @param commandName the command name
   */
  public CommandState(String name, String commandName) {
    super(name, COMMAND.name());
    this.commandName = commandName;
    this.setRequiredContextElementType(ContextElementType.INSTANCE);
  }

  /**
   * Instantiates a new Command state.
   *
   * @param name the name
   */
  public CommandState(String name) {
    super(name, COMMAND.name());
    this.setRequiredContextElementType(ContextElementType.INSTANCE);
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
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();
    String activityId = null;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();

    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
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
      Service service = serviceResourceService.get(appId, serviceTemplate.getServiceId());
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
      Command command = serviceCommand != null ? serviceCommand.getCommand() : null;
      if (command == null) {
        throw new WingsException(format(""
                                         + "Unable to find command %s for service %s",
                                     actualCommand, service.getUuid()),
            WingsException.USER);
      }

      command.setGraph(null);

      Application application = appService.get(serviceInstance.getAppId());
      String accountId = application.getAccountId();
      Artifact artifact = null;

      if (command.isArtifactNeeded()) {
        artifact = findArtifact(service.getUuid(), context);
      }

      Map<String, String> serviceVariables = context.getServiceVariables().entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
      Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();

      if (serviceVariables != null) {
        serviceVariables.replaceAll((name, value) -> context.renderExpression(value));
      }

      if (safeDisplayServiceVariables != null) {
        safeDisplayServiceVariables.replaceAll((name, value) -> context.renderExpression(value));
      }

      DeploymentType deploymentType = serviceResourceService.getDeploymentType(infrastructureMapping, service, null);

      List<CommandUnit> flattenCommandUnits =
          getFlattenCommandUnits(appId, envId, service, deploymentType.name(), accountId);

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

      if (artifact != null) {
        logger.info("Artifact being used: {} for stateExecutionInstanceId: {}", artifact.getUuid(),
            context.getStateExecutionInstanceId());
        commandExecutionContextBuilder.withMetadata(artifact.getMetadata());
        // Observed NPE in alerts
        ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
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
          artifactStreamAttributes.setArtifactServerEncryptedDataDetails(secretManager.getEncryptionDetails(
              (EncryptableSetting) artifactStreamAttributes.getServerSetting().getValue(), context.getAppId(),
              context.getWorkflowExecutionId()));
          commandExecutionContextBuilder.withArtifactServerEncryptedDataDetails(secretManager.getEncryptionDetails(
              (EncryptableSetting) artifactStreamAttributes.getServerSetting().getValue(), context.getAppId(),
              context.getWorkflowExecutionId()));
        }
        artifactStreamAttributes.setMedatadataOnly(artifactStream.isMetadataOnly());
        artifactStreamAttributes.setMetadata(artifact.getMetadata());

        if (featureFlagService.isEnabled(FeatureName.COPY_ARTIFACT, accountId)) {
          artifactStreamAttributes.setCopyArtifactEnabledForArtifactory(true);
        }
        artifactStreamAttributes.setArtifactType(service.getArtifactType());
        commandExecutionContextBuilder.withArtifactStreamAttributes(artifactStreamAttributes);

        commandExecutionContextBuilder.withArtifactFiles(artifact.getArtifactFiles());
        executionDataBuilder.withArtifactName(artifact.getDisplayName()).withActivityId(artifact.getUuid());
      } else if (command.isArtifactNeeded()) {
        throw new WingsException(
            format("Unable to find artifact for service %s", service.getName()), WingsException.USER);
      }

      Activity activity = activityHelperService.createAndSaveActivity(
          context, Type.Command, command.getName(), command.getCommandUnitType().name(), flattenCommandUnits, artifact);
      activityId = activity.getUuid();

      executionDataBuilder.withActivityId(activityId);
      expandCommand(serviceInstance, command, service.getUuid(), envId);
      executionDataBuilder.withTemplateVariable(convertToVariableMap(command.getTemplateVariables()));
      renderCommandString(command, context, executionDataBuilder.build(), artifact);
      if (featureFlagService.isEnabled(FeatureName.INLINE_SSH_COMMAND, accountId)) {
        commandExecutionContextBuilder.withInlineSshCommand(true);
      }
      CommandExecutionContext commandExecutionContext =
          commandExecutionContextBuilder.withActivityId(activityId).withDeploymentType(deploymentType.name()).build();

      DelegateTask delegateTask = DelegateTask.builder()
                                      .async(true)
                                      .accountId(accountId)
                                      .appId(appId)
                                      .taskType(TaskType.COMMAND.name())
                                      .waitId(activityId)
                                      .tags(awsCommandHelper.getAwsConfigTagsFromContext(commandExecutionContext))
                                      .parameters(new Object[] {command, commandExecutionContext})
                                      .envId(envId)
                                      .timeout(TimeUnit.MINUTES.toMillis(30))
                                      .infrastructureMappingId(infrastructureMappingId)
                                      .build();

      if (getTimeoutMillis() != null) {
        delegateTask.setTimeout(getTimeoutMillis());
      }
      delegateTaskId = delegateService.queueTask(delegateTask);
      logger.info("DelegateTaskId [{}] sent for activityId [{}]", delegateTaskId, activityId);
    } catch (Exception e) {
      if (e instanceof WingsException) {
        logger.warn("Exception in command execution", e);
      } else {
        // unhandled exception
        logger.error("Exception in command execution", e);
      }
      handleCommandException(context, activityId, appId);
      updateWorkflowExecutionStats(ExecutionStatus.FAILED, context);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withStateExecutionData(executionDataBuilder.build())
          .withErrorMessage(ExceptionUtils.getMessage(e))
          .build();
    }

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withStateExecutionData(executionDataBuilder.withDelegateTaskId(delegateTaskId).build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  static void renderCommandString(Command command, ExecutionContext context,
      CommandStateExecutionData commandStateExecutionData, Artifact artifact) {
    for (CommandUnit commandUnit : command.getCommandUnits()) {
      if (CommandUnitType.COMMAND.equals(commandUnit.getCommandUnitType())) {
        Command commandCommandUnit = (Command) commandUnit;
        commandStateExecutionData.setTemplateVariable(convertToVariableMap(commandCommandUnit.getTemplateVariables()));
        renderCommandString((Command) commandUnit, context, commandStateExecutionData, artifact);
        continue;
      }

      if (commandUnit instanceof ScpCommandUnit) {
        ScpCommandUnit scpCommandUnit = (ScpCommandUnit) commandUnit;
        if (isNotEmpty(scpCommandUnit.getDestinationDirectoryPath())) {
          scpCommandUnit.setDestinationDirectoryPath(context.renderExpression(
              scpCommandUnit.getDestinationDirectoryPath(), commandStateExecutionData, artifact));
        }
      }

      if (!(commandUnit instanceof ExecCommandUnit)) {
        continue;
      }
      ExecCommandUnit execCommandUnit = (ExecCommandUnit) commandUnit;
      if (isNotEmpty(execCommandUnit.getCommandPath())) {
        execCommandUnit.setCommandPath(
            context.renderExpression(execCommandUnit.getCommandPath(), commandStateExecutionData, artifact));
      }
      if (isNotEmpty(execCommandUnit.getCommandString())) {
        execCommandUnit.setCommandString(
            context.renderExpression(execCommandUnit.getCommandString(), commandStateExecutionData, artifact));
      }
      if (isNotEmpty(execCommandUnit.getTailPatterns())) {
        renderTailFilePattern(context, commandStateExecutionData, artifact, execCommandUnit);
      }
    }
  }

  static void renderTailFilePattern(ExecutionContext context, CommandStateExecutionData commandStateExecutionData,
      Artifact artifact, ExecCommandUnit execCommandUnit) {
    List<TailFilePatternEntry> filePatternEntries = execCommandUnit.getTailPatterns();
    for (TailFilePatternEntry filePatternEntry : filePatternEntries) {
      if (isNotEmpty(filePatternEntry.getFilePath())) {
        filePatternEntry.setFilePath(
            context.renderExpression(filePatternEntry.getFilePath(), commandStateExecutionData, artifact));
      }
      if (isNotEmpty(filePatternEntry.getPattern())) {
        filePatternEntry.setPattern(
            context.renderExpression(filePatternEntry.getPattern(), commandStateExecutionData, artifact));
      }
    }
    execCommandUnit.setTailPatterns(filePatternEntries);
  }

  private List<CommandUnit> getFlattenCommandUnits(
      String appId, String envId, Service service, String deploymentType, String accountId) {
    List<CommandUnit> flattenCommandUnitList =
        serviceResourceService.getFlattenCommandUnitList(appId, service.getUuid(), envId, commandName);

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
    return flattenCommandUnitList;
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
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("Unexpected number of response data items")
          .build();
    }

    ResponseData notifyResponseData = response.values().iterator().next();

    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage())
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
        commandExecutionResult.getStatus().equals(SUCCESS) ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED);

    ExecutionStatus executionStatus =
        commandExecutionResult.getStatus().equals(SUCCESS) ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    updateWorkflowExecutionStats(executionStatus, context);

    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();
    commandStateExecutionData.setStatus(executionStatus);
    on(commandStateExecutionData).set("activityService", activityService);
    commandStateExecutionData.setCountsByStatuses(
        (CountsByStatuses) commandStateExecutionData.getExecutionSummary().get("breakdown").getValue());
    commandStateExecutionData.setDelegateMetaInfo(commandExecutionResult.getDelegateMetaInfo());

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(commandExecutionResult.getErrorMessage())
        .withStateExecutionData(commandStateExecutionData)
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
      if (CommandUnitType.COMMAND.equals(commandUnit.getCommandUnitType())) {
        expandCommand(serviceInstance, (Command) commandUnit, serviceId, envId);
      }
    }
  }

  @Override
  @SchemaIgnore
  public List<EntityType> getRequiredExecutionArgumentTypes() {
    return Lists.newArrayList(EntityType.SERVICE, EntityType.INSTANCE);
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
