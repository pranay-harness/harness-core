package software.wings.service.impl;

import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.CommandUnitType.COMMAND;
import static software.wings.beans.ErrorCodes.COMMAND_DOES_NOT_EXIST;

import software.wings.beans.Activity;
import software.wings.beans.Command;
import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.ServiceInstance;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 6/2/16.
 */
public class ServiceCommandExecutorServiceImpl implements ServiceCommandExecutorService {
  /**
   * The Activity service.
   */
  @Inject ActivityService activityService;
  /**
   * The Command unit executor service.
   */
  @Inject CommandUnitExecutorService commandUnitExecutorService;

  @Inject private ServiceResourceService serviceResourceService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceCommandExecutorService#execute(software.wings.beans.ServiceInstance,
   * software.wings.beans.Command)
   */
  @Override
  public ExecutionResult execute(ServiceInstance serviceInstance, Command command) {
    Activity activity = getPersistedActivity(serviceInstance, command);
    return executeCommand(serviceInstance, command, activity);
  }

  private ExecutionResult executeCommand(ServiceInstance serviceInstance, Command command, Activity activity) {
    Command executableCommand = command;
    if (command.getReferenceId() != null) {
      executableCommand =
          serviceResourceService.getCommandByName(command.getAppId(), command.getServiceId(), command.getReferenceId());
      if (executableCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
    }
    List<CommandUnit> commandUnits = executableCommand.getCommandUnits();
    executableCommand.setExecutionResult(SUCCESS);

    for (CommandUnit commandUnit : commandUnits) {
      ExecutionResult executionResult = COMMAND.equals(commandUnit.getCommandUnitType())
          ? executeCommand(serviceInstance, (Command) commandUnit, activity)
          : executeCommandUnit(serviceInstance, activity, commandUnit);
      commandUnit.setExecutionResult(executionResult);
      if (executionResult.equals(FAILURE)) {
        executableCommand.setExecutionResult(FAILURE);
        break;
      }
    }
    return executableCommand.getExecutionResult();
  }

  private ExecutionResult executeCommandUnit(
      ServiceInstance serviceInstance, Activity activity, CommandUnit commandUnit) {
    return commandUnitExecutorService.execute(serviceInstance.getHost(), commandUnit, activity.getUuid());
  }

  private Activity getPersistedActivity(ServiceInstance serviceInstance, Command command) {
    Activity activity = anActivity()
                            .withAppId(serviceInstance.getAppId())
                            .withEnvironmentId(serviceInstance.getEnvId())
                            .withServiceTemplateId(serviceInstance.getServiceTemplate().getUuid())
                            .withServiceTemplateName(serviceInstance.getServiceTemplate().getName())
                            .withServiceId(serviceInstance.getServiceTemplate().getService().getUuid())
                            .withServiceName(serviceInstance.getServiceTemplate().getService().getName())
                            .withCommandName(command.getName())
                            .withCommandType(command.getCommandUnitType().name())
                            .withHostName(serviceInstance.getHost().getHostName())
                            .build();

    if (serviceInstance.getRelease() != null) {
      activity.setReleaseId(serviceInstance.getRelease().getUuid());
      activity.setReleaseName(serviceInstance.getRelease().getReleaseName());
    }
    if (serviceInstance.getArtifact() != null) {
      activity.setArtifactName(serviceInstance.getArtifact().getDisplayName());
    }
    activity = activityService.save(activity);
    return activity;
  }
}
