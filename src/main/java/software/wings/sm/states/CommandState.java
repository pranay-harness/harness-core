package software.wings.sm.states;

import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.COMMAND;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;
import software.wings.beans.Artifact;
import software.wings.beans.Command;
import software.wings.beans.CommandExecutionContext;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
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

import java.util.Optional;

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

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient ServiceInstanceService serviceInstanceService;

  @Inject @Transient private transient ServiceCommandExecutorService serviceCommandExecutorService;

  @Inject @Transient private transient ActivityService activityService;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient EnvironmentService environmentService;

  @Inject @Transient private transient WorkflowService workflowService;

  @Attributes(title = "Command")
  @Expand(dataProvider = CommandStateDataProvider.class)
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
    ExecutionStatus executionStatus;
    String activityId = null;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();
    Environment environment = environmentService.get(appId, envId);

    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);

    updateWorflowExecutionStatsInProgress(context);

    try {
      if (instanceElement == null) {
        throw new StateExecutionException("No InstanceElement present in context");
      }

      ServiceInstance serviceInstance = serviceInstanceService.get(appId, envId, instanceElement.getUuid());

      if (serviceInstance == null) {
        throw new StateExecutionException("Unable to find service instance");
      }

      Service service = serviceInstance.getServiceTemplate().getService();

      executionDataBuilder.withServiceId(service.getUuid())
          .withServiceName(service.getName())
          .withTemplateId(serviceInstance.getServiceTemplate().getUuid())
          .withTemplateName(serviceInstance.getServiceTemplate().getName())
          .withHostId(serviceInstance.getHost().getInfraId())
          .withHostName(serviceInstance.getHost().getHostName());

      String actualCommand = commandName;
      try {
        actualCommand = context.renderExpression(commandName);
      } catch (Exception e) {
        e.printStackTrace();
      }

      Command command = serviceResourceService.getCommandByName(appId, service.getUuid(), actualCommand);

      if (command == null) {
        throw new StateExecutionException(
            String.format("Unable to find command %s for service %s", actualCommand, service.getName()));
      }

      executionDataBuilder.withCommandName(command.getName());

      Activity.Builder activityBuilder = anActivity()
                                             .withAppId(serviceInstance.getAppId())
                                             .withEnvironmentId(environment.getUuid())
                                             .withEnvironmentName(environment.getName())
                                             .withEnvironmentType(environment.getEnvironmentType())
                                             .withServiceTemplateId(serviceInstance.getServiceTemplate().getUuid())
                                             .withServiceTemplateName(serviceInstance.getServiceTemplate().getName())
                                             .withServiceId(service.getUuid())
                                             .withServiceName(service.getName())
                                             .withCommandName(command.getName())
                                             .withCommandType(command.getCommandUnitType().name())
                                             .withHostName(serviceInstance.getHost().getHostName());

      String backupPath = getEvaluatedSettingValue(context, appId, envId, BACKUP_PATH);
      String runtimePath = getEvaluatedSettingValue(context, appId, envId, RUNTIME_PATH);
      String stagingPath = getEvaluatedSettingValue(context, appId, envId, STAGING_PATH);

      CommandExecutionContext.Builder commandExecutionContextBuilder =
          aCommandExecutionContext()
              .withAppId(appId)
              .withBackupPath(backupPath)
              .withRuntimePath(runtimePath)
              .withStagingPath(stagingPath)
              .withExecutionCredential(workflowStandardParams.getExecutionCredential());

      if (command.isArtifactNeeded()) {
        Artifact artifact =
            workflowStandardParams.getArtifactForService(serviceInstance.getServiceTemplate().getService());
        if (artifact == null) {
          throw new StateExecutionException(String.format("Unable to find artifact for service %s", service.getName()));
        }
        activityBuilder.withReleaseId(artifact.getRelease().getUuid())
            .withReleaseName(artifact.getRelease().getReleaseName())
            .withArtifactName(artifact.getDisplayName());
        commandExecutionContextBuilder.withArtifact(artifact);
        executionDataBuilder.withArtifactName(artifact.getDisplayName()).withActivityId(artifact.getUuid());
      }

      Activity activity = activityService.save(activityBuilder.build());
      activityId = activity.getUuid();

      executionDataBuilder.withActivityId(activityId);

      CommandExecutionContext commandExecutionContext =
          commandExecutionContextBuilder.withActivityId(activityId).build();
      ExecutionResult executionResult =
          serviceCommandExecutorService.execute(serviceInstance, command, commandExecutionContext);

      activityService.updateStatus(
          activityId, appId, executionResult.equals(SUCCESS) ? Status.COMPLETED : Status.FAILED);

      if (executionResult.equals(SUCCESS) && command.isArtifactNeeded()) {
        serviceInstance.setRelease(commandExecutionContext.getArtifact().getRelease());
        serviceInstance.setArtifact(commandExecutionContext.getArtifact());
        serviceInstanceService.update(serviceInstance);
      }

      executionStatus = executionResult.equals(SUCCESS) ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    } catch (Exception e) {
      if (activityId != null) {
        activityService.updateStatus(activityId, appId, Status.FAILED);
      }
      updateWorflowExecutionStats(ExecutionStatus.FAILED, context);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withStateExecutionData(executionDataBuilder.build())
          .withErrorMessage(e.getMessage())
          .build();
    }
    updateWorflowExecutionStats(executionStatus, context);
    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(executionDataBuilder.build())
        .build();
  }

  private void updateWorflowExecutionStats(ExecutionStatus executionStatus, ExecutionContext context) {
    Optional<ContextElement> simpleWorkflowParamOpt =
        context.getContextElementList(ContextElementType.PARAM)
            .stream()
            .filter(contextElement -> contextElement instanceof SimpleWorkflowParam)
            .findFirst();
    if (simpleWorkflowParamOpt.isPresent()) {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();
      if (executionStatus == ExecutionStatus.FAILED) {
        workflowService.incrementFailed(appId, context.getWorkflowExecutionId(), 1);
      } else {
        workflowService.incrementSuccess(appId, context.getWorkflowExecutionId(), 1);
      }
    }
  }

  private void updateWorflowExecutionStatsInProgress(ExecutionContext context) {
    Optional<ContextElement> simpleWorkflowParamOpt =
        context.getContextElementList(ContextElementType.PARAM)
            .stream()
            .filter(contextElement -> contextElement instanceof SimpleWorkflowParam)
            .findFirst();
    if (simpleWorkflowParamOpt.isPresent()) {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();
      workflowService.incrementInProgressCount(appId, context.getWorkflowExecutionId(), 1);
    }
  }

  private String getEvaluatedSettingValue(ExecutionContext context, String appId, String envId, String variable) {
    SettingAttribute settingAttribute = settingsService.getByName(appId, envId, variable);
    StringValue stringValue = (StringValue) settingAttribute.getValue();
    String settingValue = stringValue.getValue();
    try {
      settingValue = context.renderExpression(settingValue);
    } catch (Exception e) {
      // ignore
    }
    return settingValue;
  }
}
