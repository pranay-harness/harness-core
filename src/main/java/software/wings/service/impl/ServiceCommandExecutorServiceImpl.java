package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.command.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import software.wings.beans.ServiceInstance;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.beans.command.InitCommandUnit;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

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
  @Inject private CommandUnitExecutorService commandUnitExecutorService;

  @Inject private ServiceResourceService serviceResourceService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceCommandExecutorService#execute(software.wings.beans.ServiceInstance,
   * software.wings.beans.command.Command)
   */
  @Override
  public ExecutionResult execute(ServiceInstance serviceInstance, Command command, CommandExecutionContext context) {
    try {
      ExecutionResult executionResult = executeCommand(serviceInstance, command, context);
      commandUnitExecutorService.cleanup(context.getActivityId(), serviceInstance.getHost());
      return executionResult;
    } catch (Exception ex) {
      commandUnitExecutorService.cleanup(context.getActivityId(), serviceInstance.getHost());
      throw ex;
    }
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
    commandUnits.stream()
        .filter(commandUnit -> commandUnit instanceof InitCommandUnit)
        .forEach(commandUnit -> ((InitCommandUnit) commandUnit).setCommand(command));

    for (CommandUnit commandUnit : commandUnits) {
      ExecutionResult executionResult = COMMAND.equals(commandUnit.getCommandUnitType())
          ? executeCommand(serviceInstance, (Command) commandUnit, context)
          : commandUnitExecutorService.execute(serviceInstance.getHost(), commandUnit, context);
      commandUnit.setExecutionResult(executionResult);
      if (executionResult.equals(FAILURE)) {
        executableCommand.setExecutionResult(FAILURE);
        break;
      }
    }
    return executableCommand.getExecutionResult();
  }
}
