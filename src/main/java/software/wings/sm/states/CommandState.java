package software.wings.sm.states;

import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.StateType.COMMAND;

import com.google.inject.Inject;

import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;
import software.wings.beans.Artifact;
import software.wings.beans.Command;
import software.wings.beans.CommandExecutionContext;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

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

  private static final long serialVersionUID = -6767922416807341483L;

  @Inject private transient ServiceResourceService serviceResourceService;

  @Inject private transient ServiceInstanceService serviceInstanceService;

  @Inject private transient ServiceCommandExecutorService serviceCommandExecutorService;

  @Inject private transient ActivityService activityService;

  @Inject private transient SettingsService settingsService;

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

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();

    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);
    ServiceInstance serviceInstance = serviceInstanceService.get(appId, envId, instanceElement.getUuid());

    Service service = serviceInstance.getServiceTemplate().getService();

    String actualCommand = commandName;
    try {
      actualCommand = context.renderExpression(commandName);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Command command = serviceResourceService.getCommandByName(appId, service.getUuid(), actualCommand);

    Activity.Builder activityBuilder = anActivity()
                                           .withAppId(serviceInstance.getAppId())
                                           .withEnvironmentId(serviceInstance.getEnvId())
                                           .withServiceTemplateId(serviceInstance.getServiceTemplate().getUuid())
                                           .withServiceTemplateName(serviceInstance.getServiceTemplate().getName())
                                           .withServiceId(service.getUuid())
                                           .withServiceName(service.getName())
                                           .withCommandName(command.getName())
                                           .withCommandType(command.getCommandUnitType().name())
                                           .withHostName(serviceInstance.getHost().getHostName());

    CommandExecutionContext.Builder commandExecutionContextBuilder =
        aCommandExecutionContext()
            .withAppId(appId)
            .withBackupPath(getEvaluatedSettingValue(context, appId, envId, BACKUP_PATH))
            .withRuntimePath(getEvaluatedSettingValue(context, appId, envId, RUNTIME_PATH))
            .withStagingPath(getEvaluatedSettingValue(context, appId, envId, STAGING_PATH))
            .withExecutionCredential(workflowStandardParams.getExecutionCredential());

    if (command.isArtifactNeeded()) {
      Artifact artifact =
          workflowStandardParams.getArtifactForService(serviceInstance.getServiceTemplate().getService());
      activityBuilder.withReleaseId(artifact.getRelease().getUuid())
          .withReleaseName(artifact.getRelease().getReleaseName())
          .withArtifactName(artifact.getDisplayName());
      commandExecutionContextBuilder.withArtifact(artifact);
    }

    Activity activity = activityService.save(activityBuilder.build());

    ExecutionResult executionResult = serviceCommandExecutorService.execute(
        serviceInstance, command, commandExecutionContextBuilder.withActivityId(activity.getUuid()).build());

    activityService.updateStatus(
        activity.getUuid(), appId, executionResult.equals(SUCCESS) ? Status.COMPLETED : Status.FAILED);
    return anExecutionResponse()
        .withExecutionStatus(executionResult.equals(SUCCESS) ? ExecutionStatus.SUCCESS : FAILED)
        .build();
  }

  private String getEvaluatedSettingValue(ExecutionContext context, String appId, String envId, String variable) {
    SettingAttribute settingAttribute = settingsService.getByName(appId, envId, variable);
    StringValue stringValue = (StringValue) settingAttribute.getValue();
    String settingValue = stringValue.getValue();
    return context.renderExpression(settingValue);
  }
}
