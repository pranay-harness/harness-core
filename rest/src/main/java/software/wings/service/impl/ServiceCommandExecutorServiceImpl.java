package software.wings.service.impl;

import static software.wings.beans.command.AbstractCommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import com.google.inject.Singleton;

import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;

import java.util.List;
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
  @Inject private CommandUnitExecutorService commandUnitExecutorService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceCommandExecutorService#execute(software.wings.beans.ServiceInstance,
   * software.wings.beans.command.Command)
   */
  @Override
  public ExecutionResult execute(Command command, CommandExecutionContext context) {
    try {
      InitSshCommandUnit initCommandUnit = new InitSshCommandUnit();
      initCommandUnit.setCommand(command);
      command.getCommandUnits().add(0, initCommandUnit);
      command.getCommandUnits().add(new CleanupSshCommandUnit());
      ExecutionResult executionResult = executeCommand(command, context);
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      return executionResult;
    } catch (Exception ex) {
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      ex.printStackTrace();
      throw ex;
    }
  }

  private ExecutionResult executeCommand(Command command, CommandExecutionContext context) {
    List<CommandUnit> commandUnits = command.getCommandUnits();

    ExecutionResult executionResult = ExecutionResult.FAILURE;

    for (CommandUnit commandUnit : commandUnits) {
      executionResult = COMMAND.equals(commandUnit.getCommandUnitType())
          ? executeCommand((Command) commandUnit, context)
          : commandUnitExecutorService.execute(context.getHost(), commandUnit, context);
      if (FAILURE == executionResult) {
        break;
      }
    }

    return executionResult;
  }
}
