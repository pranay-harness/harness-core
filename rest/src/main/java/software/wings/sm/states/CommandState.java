package software.wings.sm.states;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.joor.Reflect.on;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.ErrorCode.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.COMMAND;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceInstanceArtifactParam;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
public class CommandState extends State {
  /**
   * The constant RUNTIME_PATH.
   */
  public static final String RUNTIME_PATH = "RUNTIME_PATH";
  /**
   * The constant BACKUP_PATH.
   */
  public static final String BACKUP_PATH = "BACKUP_PATH";
  /**
   * The constant STAGING_PATH.
   */
  public static final String STAGING_PATH = "STAGING_PATH";

  private static final Logger logger = LoggerFactory.getLogger(CommandState.class);

  @Inject @Transient private transient AppService appService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient ServiceInstanceService serviceInstanceService;

  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;

  @Inject @Transient private transient HostService hostService;

  @Inject @Transient private transient ActivityService activityService;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient EnvironmentService environmentService;

  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;

  @Inject @Transient private ArtifactStreamService artifactStreamService;

  @Inject @Transient private DelegateService delegateService;

  @Inject @Transient @SchemaIgnore private transient ExecutorService executorService;

  @Transient @Inject private transient ArtifactService artifactService;

  @Transient @Inject private transient SecretManager secretManager;

  @Attributes(title = "Command")
  @Expand(dataProvider = CommandStateEnumDataProvider.class)
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  private String commandName;

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

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();
    String activityId = null;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();
    Environment environment = environmentService.get(appId, envId, false);

    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);

    updateWorflowExecutionStatsInProgress(context);

    String delegateTaskId;
    try {
      if (instanceElement == null) {
        throw new StateExecutionException("No InstanceElement present in context");
      }

      ServiceInstance serviceInstance = serviceInstanceService.get(appId, envId, instanceElement.getUuid());

      if (serviceInstance == null) {
        throw new StateExecutionException("Unable to find service instance");
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
        e.printStackTrace();
      }

      executionDataBuilder.withCommandName(actualCommand);
      ServiceCommand serviceCommand =
          serviceResourceService.getCommandByName(appId, service.getUuid(), envId, actualCommand);
      Command command = serviceCommand != null ? serviceCommand.getCommand() : null;

      if (command == null) {
        throw new StateExecutionException(
            String.format("Unable to find command %s for service %s", actualCommand, service.getName()));
      }

      Application application = appService.get(serviceInstance.getAppId());
      String accountId = application.getAccountId();

      Map<String, String> serviceVariables = context.getServiceVariables();
      Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();

      ActivityBuilder activityBuilder = Activity.builder()
                                            .applicationName(application.getName())
                                            .environmentId(environment.getUuid())
                                            .environmentName(environment.getName())
                                            .environmentType(environment.getEnvironmentType())
                                            .serviceTemplateId(serviceTemplateId)
                                            .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
                                            .serviceId(service.getUuid())
                                            .serviceName(service.getName())
                                            .commandName(command.getName())
                                            .type(Type.Command)
                                            .serviceInstanceId(serviceInstance.getUuid())
                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                            .workflowType(context.getWorkflowType())
                                            .workflowId(context.getWorkflowId())
                                            .workflowExecutionName(context.getWorkflowExecutionName())
                                            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                                            .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                                            .commandType(command.getCommandUnitType().name())
                                            .hostName(host.getHostName())
                                            .publicDns(host.getPublicDns())
                                            .commandUnits(getFlattenCommandUnits(appId, envId, service, command))
                                            .serviceVariables(serviceVariables)
                                            .status(ExecutionStatus.RUNNING);

      String backupPath = getEvaluatedSettingValue(context, accountId, appId, envId, BACKUP_PATH).replace(" ", "\\ ");
      String runtimePath = getEvaluatedSettingValue(context, accountId, appId, envId, RUNTIME_PATH).replace(" ", "\\ ");
      String stagingPath = getEvaluatedSettingValue(context, accountId, appId, envId, STAGING_PATH).replace(" ", "\\ ");

      CommandExecutionContext.Builder commandExecutionContextBuilder =
          aCommandExecutionContext()
              .withAppId(appId)
              .withEnvId(envId)
              .withBackupPath(backupPath)
              .withRuntimePath(runtimePath)
              .withStagingPath(stagingPath)
              .withExecutionCredential(workflowStandardParams.getExecutionCredential())
              .withServiceVariables(serviceVariables)
              .withSafeDisplayServiceVariables(safeDisplayServiceVariables)
              .withHost(host)
              .withServiceTemplateId(serviceTemplateId)
              .withAppContainer(service.getAppContainer())
              .withAccountId(accountId);

      if (isNotEmpty(host.getHostConnAttr())) {
        SettingAttribute hostConnectionAttribute = settingsService.get(host.getHostConnAttr());
        commandExecutionContextBuilder.withHostConnectionAttributes(hostConnectionAttribute);
        commandExecutionContextBuilder.withHostConnectionCredentials(secretManager.getEncryptionDetails(
            (Encryptable) hostConnectionAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId()));
      }
      if (isNotEmpty(host.getBastionConnAttr())) {
        SettingAttribute bastionConnectionAttribute = settingsService.get(host.getBastionConnAttr());
        commandExecutionContextBuilder.withBastionConnectionAttributes(bastionConnectionAttribute);
        commandExecutionContextBuilder.withBastionConnectionCredentials(secretManager.getEncryptionDetails(
            (Encryptable) bastionConnectionAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId()));
      }

      Artifact artifact = findArtifact(context, service.getUuid());
      if (artifact != null) {
        commandExecutionContextBuilder.withMetadata(artifact.getMetadata());
        ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
        if (artifactStream.getArtifactStreamType().equals(DOCKER.name())
            || artifactStream.getArtifactStreamType().equals(ECR.name())
            || artifactStream.getArtifactStreamType().equals(GCR.name())) {
          ArtifactStreamAttributes artifactStreamAttributes = artifactStream.getArtifactStreamAttributes();
          artifactStreamAttributes.setServerSetting(settingsService.get(artifactStream.getSettingId()));
          commandExecutionContextBuilder.withArtifactStreamAttributes(artifactStreamAttributes);
        }
        activityBuilder.artifactStreamId(artifactStream.getUuid())
            .artifactStreamName(artifactStream.getSourceName())
            .artifactName(artifact.getDisplayName())
            .artifactId(artifact.getUuid());
        commandExecutionContextBuilder.withArtifactFiles(artifact.getArtifactFiles());
        executionDataBuilder.withArtifactName(artifact.getDisplayName()).withActivityId(artifact.getUuid());
      } else if (command.isArtifactNeeded()) {
        throw new StateExecutionException(String.format("Unable to find artifact for service %s", service.getName()));
      }
      Activity act = activityBuilder.build();
      act.setAppId(application.getUuid());
      Activity activity = activityService.save(act);
      activityId = activity.getUuid();

      executionDataBuilder.withActivityId(activityId);
      expandCommand(serviceInstance, command, service.getUuid(), envId);
      renderCommandString(command, context);
      CommandExecutionContext commandExecutionContext =
          commandExecutionContextBuilder.withActivityId(activityId).build();

      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
      delegateTaskId = delegateService.queueTask(aDelegateTask()
                                                     .withAccountId(accountId)
                                                     .withAppId(appId)
                                                     .withTaskType(TaskType.COMMAND)
                                                     .withWaitId(activityId)
                                                     .withParameters(new Object[] {command, commandExecutionContext})
                                                     .withEnvId(envId)
                                                     .withTimeout(TimeUnit.MINUTES.toMillis(30))
                                                     .withInfrastructureMappingId(infrastructureMappingId)
                                                     .build());
      logger.info("DelegateTaskId [{}] sent for activityId [{}]", delegateTaskId, activityId);
    } catch (Exception e) {
      logger.error("Exception in command execution", e);
      handleCommandException(context, activityId, appId);
      updateWorkflowExecutionStats(ExecutionStatus.FAILED, context);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withStateExecutionData(executionDataBuilder.build())
          .withErrorMessage(e.getMessage())
          .build();
    }

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withStateExecutionData(executionDataBuilder.withDelegateTaskId(delegateTaskId).build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private void renderCommandString(Command command, ExecutionContext context) {
    for (CommandUnit commandUnit : command.getCommandUnits()) {
      if (commandUnit.getCommandUnitType() != CommandUnitType.EXEC || !(commandUnit instanceof ExecCommandUnit)
          || ((ExecCommandUnit) commandUnit).getCommandString() == null) {
        continue;
      }
      ((ExecCommandUnit) commandUnit)
          .setCommandString(context.renderExpression(((ExecCommandUnit) commandUnit).getCommandString()));
    }
  }

  private List<CommandUnit> getFlattenCommandUnits(String appId, String envId, Service service, Command command) {
    List<CommandUnit> flattenCommandUnitList =
        serviceResourceService.getFlattenCommandUnitList(appId, service.getUuid(), envId, commandName);
    if (DeploymentType.SSH.name().equals(command.getDeploymentType())) {
      flattenCommandUnitList.add(0, new InitSshCommandUnit());
      flattenCommandUnitList.add(new CleanupSshCommandUnit());
    }
    return flattenCommandUnitList;
  }

  private Artifact findArtifact(ExecutionContext context, String serviceId) {
    ContextElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);
    if (instanceElement != null) {
      ServiceInstanceArtifactParam serviceArtifactElement =
          context.getContextElement(ContextElementType.PARAM, Constants.SERVICE_INSTANCE_ARTIFACT_PARAMS);
      if (serviceArtifactElement != null) {
        String artifactId = serviceArtifactElement.getInstanceArtifactMap().get(instanceElement.getUuid());
        if (artifactId != null) {
          return artifactService.get(context.getAppId(), artifactId);
        }
      }
    }

    return ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
  }

  private void handleCommandException(ExecutionContext context, String activityId, String appId) {
    if (activityId != null) {
      activityService.updateStatus(activityId, appId, ExecutionStatus.FAILED);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();

    CommandExecutionResult commandExecutionResult = null;
    String activityId = null;
    for (Object status : response.values()) {
      commandExecutionResult = (CommandExecutionResult) status;
    }

    for (String key : response.keySet()) {
      activityId = key;
    }

    if (commandExecutionResult.getStatus() != SUCCESS && isNotEmpty(commandExecutionResult.getErrorMessage())) {
      handleCommandException(context, activityId, appId);
    }

    activityService.updateStatus(activityId, appId,
        commandExecutionResult.getStatus().equals(SUCCESS) ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED);

    ExecutionStatus executionStatus =
        commandExecutionResult.getStatus().equals(SUCCESS) ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    updateWorkflowExecutionStats(executionStatus, context);

    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();
    commandStateExecutionData.setStatus(executionStatus);
    on(commandStateExecutionData).set("activityService", activityService);
    commandStateExecutionData.setCountsByStatuses(
        (CountsByStatuses) commandStateExecutionData.getExecutionSummary().get("breakdown").getValue());

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

  private void updateWorflowExecutionStatsInProgress(ExecutionContext context) {
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
      Command referedCommand = Optional
                                   .ofNullable(serviceResourceService.getCommandByName(
                                       serviceInstance.getAppId(), serviceId, envId, command.getReferenceId()))
                                   .orElse(aServiceCommand().build())
                                   .getCommand();
      if (referedCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
      command.setCommandUnits(referedCommand.getCommandUnits());
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
