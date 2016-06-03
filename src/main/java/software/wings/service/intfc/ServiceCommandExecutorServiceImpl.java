package software.wings.service.intfc;

import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;

import software.wings.beans.Activity;
import software.wings.beans.Command;
import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.ServiceInstance;
import software.wings.service.impl.ServiceCommandExecutorService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 6/2/16.
 */
public class ServiceCommandExecutorServiceImpl implements ServiceCommandExecutorService {
  @Inject ActivityService activityService;
  @Inject CommandUnitExecutorService commandUnitExecutorService;

  @Override
  public ExecutionResult execute(ServiceInstance serviceInstance, Command command) {
    Activity activity = getPersistedActivity(serviceInstance, command);
    List<CommandUnit> commandUnits = command.getCommandUnits();
    command.setExecutionResult(SUCCESS);
    for (CommandUnit commandUnit : commandUnits) {
      ExecutionResult executionResult =
          commandUnitExecutorService.execute(serviceInstance.getHost(), commandUnit, activity.getUuid());
      commandUnit.setExecutionResult(executionResult);
      if (executionResult.equals(FAILURE)) {
        command.setExecutionResult(FAILURE);
        break;
      }
    }
    return command.getExecutionResult();
  }

  private Activity getPersistedActivity(ServiceInstance serviceInstance, Command command) {
    Activity activity = anActivity()
                            .withAppId(serviceInstance.getAppId())
                            .withServiceTemplateId(serviceInstance.getServiceTemplate().getUuid())
                            .withServiceTemplateName(serviceInstance.getServiceTemplate().getName())
                            .withServiceId(serviceInstance.getService().getUuid())
                            .withServiceName(serviceInstance.getService().getName())
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
