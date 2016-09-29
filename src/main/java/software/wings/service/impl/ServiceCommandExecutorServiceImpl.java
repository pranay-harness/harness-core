package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.beans.ErrorCodes.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.command.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import software.wings.beans.ServiceInstance;
import software.wings.beans.command.CleanupCommandUnit;
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
      prepareCommand(serviceInstance, command, context);
      InitCommandUnit initCommandUnit = new InitCommandUnit();
      initCommandUnit.setCommand(command);
      command.getCommandUnits().add(0, initCommandUnit);
      command.getCommandUnits().add(new CleanupCommandUnit());
      ExecutionResult executionResult = executeCommand(serviceInstance, command, context);
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      return executionResult;
    } catch (Exception ex) {
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      throw ex;
    }
  }

  private void prepareCommand(ServiceInstance serviceInstance, Command command, CommandExecutionContext context) {
    if (isNotEmpty(command.getReferenceId())) {
      Command referedCommand = serviceResourceService.getCommandByName(
          serviceInstance.getAppId(), context.getServiceTemplate().getService().getUuid(), command.getReferenceId());
      if (referedCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
      command.setCommandUnits(referedCommand.getCommandUnits());
    }

    for (CommandUnit commandUnit : command.getCommandUnits()) {
      if (COMMAND.equals(commandUnit.getCommandUnitType())) {
        prepareCommand(serviceInstance, (Command) commandUnit, context);
      }
    }
  }

  private ExecutionResult executeCommand(
      ServiceInstance serviceInstance, Command command, CommandExecutionContext context) {
    List<CommandUnit> commandUnits = command.getCommandUnits();

    ExecutionResult executionResult = ExecutionResult.FAILURE;

    for (CommandUnit commandUnit : commandUnits) {
      executionResult = COMMAND.equals(commandUnit.getCommandUnitType())
          ? executeCommand(serviceInstance, (Command) commandUnit, context)
          : commandUnitExecutorService.execute(context.getHost(), commandUnit, context);
      if (FAILURE == executionResult) {
        break;
      }
    }

    return executionResult;
  }
}
