package software.wings.service.impl;

import static software.wings.beans.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.CommandUnitType.COMMAND;
import static software.wings.beans.ErrorCodes.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;

import software.wings.beans.Command;
import software.wings.beans.CommandExecutionContext;
import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Host;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.ServiceInstance;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 6/2/16.
 */
@ValidateOnExecution
@Singleton
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
  public ExecutionResult execute(ServiceInstance serviceInstance, Command command, CommandExecutionContext context) {
    return executeCommand(serviceInstance, command, context);
  }

  private ExecutionResult executeCommand(
      ServiceInstance serviceInstance, Command command, CommandExecutionContext context) {
    Command executableCommand = command;
    if (command.getReferenceId() != null) {
      executableCommand = serviceResourceService.getCommandByName(serviceInstance.getAppId(),
          serviceInstance.getServiceTemplate().getService().getUuid(), command.getReferenceId());
      if (executableCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
    }
    List<CommandUnit> commandUnits = executableCommand.getCommandUnits();
    executableCommand.setExecutionResult(SUCCESS);

    for (CommandUnit commandUnit : commandUnits) {
      ExecutionResult executionResult = COMMAND.equals(commandUnit.getCommandUnitType())
          ? executeCommand(serviceInstance, (Command) commandUnit, context)
          : executeCommandUnit(serviceInstance, context, commandUnit);
      commandUnit.setExecutionResult(executionResult);
      if (executionResult.equals(FAILURE)) {
        executableCommand.setExecutionResult(FAILURE);
        break;
      }
    }
    return executableCommand.getExecutionResult();
  }

  private ExecutionResult executeCommandUnit(
      ServiceInstance serviceInstance, CommandExecutionContext context, CommandUnit commandUnit) {
    commandUnit.setup(context);
    Host host = serviceInstance.getHost();
    host.setHostConnectionCredential(getHostConnectionCredentialFromExecutionCredential(
        (SSHExecutionCredential) context.getExecutionCredential())); // TODO: transform not needed. merge
    return commandUnitExecutorService.execute(host, commandUnit, context.getActivityId());
  }

  private HostConnectionCredential getHostConnectionCredentialFromExecutionCredential(
      SSHExecutionCredential execCredential) {
    return aHostConnectionCredential()
        .withSshUser(execCredential.getSshUser())
        .withSshPassword(execCredential.getSshPassword())
        .withAppUser(execCredential.getAppAccount())
        .withAppUserPassword(execCredential.getAppAccountPassword())
        .build();
  }
}
