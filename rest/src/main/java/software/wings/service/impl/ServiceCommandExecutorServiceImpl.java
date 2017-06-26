package software.wings.service.impl;

import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import com.google.inject.Singleton;

import software.wings.api.DeploymentType;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 6/2/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceCommandExecutorServiceImpl implements ServiceCommandExecutorService {
  /**
   * The Command unit executor service.
   */

  @Inject private Map<String, CommandUnitExecutorService> commandUnitExecutorServiceMap;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceCommandExecutorService#execute(software.wings.beans.ServiceInstance,
   * software.wings.beans.command.Command)
   */
  @Override
  public CommandExecutionStatus execute(Command command, CommandExecutionContext context) {
    if (SSH.name().equals(command.getDeploymentType())) {
      return executeSshCommand(command, context);
    } else {
      return executeNonSshDeploymentCommand(
          command, context, commandUnitExecutorServiceMap.get(command.getDeploymentType()));
    }
  }

  private CommandExecutionStatus executeNonSshDeploymentCommand(
      Command command, CommandExecutionContext context, CommandUnitExecutorService commandUnitExecutorService) {
    try {
      CommandExecutionStatus commandExecutionStatus = commandUnitExecutorService.execute(
          context.getHost(), command.getCommandUnits().get(0), context); // TODO:: do it recursively
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      return commandExecutionStatus;
    } catch (Exception ex) {
      ex.printStackTrace();
      if (context != null) {
        commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      }
      throw ex;
    }
  }

  public CommandExecutionStatus executeSshCommand(Command command, CommandExecutionContext context) {
    CommandUnitExecutorService commandUnitExecutorService =
        commandUnitExecutorServiceMap.get(DeploymentType.SSH.name());
    try {
      InitSshCommandUnit initCommandUnit = new InitSshCommandUnit();
      initCommandUnit.setCommand(command);
      command.getCommandUnits().add(0, initCommandUnit);
      command.getCommandUnits().add(new CleanupSshCommandUnit());
      CommandExecutionStatus commandExecutionStatus = executeSshCommand(commandUnitExecutorService, command, context);
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      return commandExecutionStatus;
    } catch (Exception ex) {
      ex.printStackTrace();
      if (context != null) {
        commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      }
      throw ex;
    }
  }

  private CommandExecutionStatus executeSshCommand(
      CommandUnitExecutorService commandUnitExecutorService, Command command, CommandExecutionContext context) {
    List<CommandUnit> commandUnits = command.getCommandUnits();

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.FAILURE;

    for (CommandUnit commandUnit : commandUnits) {
      commandExecutionStatus = COMMAND.equals(commandUnit.getCommandUnitType())
          ? executeSshCommand(commandUnitExecutorService, (Command) commandUnit, context)
          : commandUnitExecutorService.execute(context.getHost(), commandUnit, context);
      if (FAILURE == commandExecutionStatus) {
        break;
      }
    }

    return commandExecutionStatus;
  }
}
