package software.wings.service.intfc;

import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;

/**
 * Created by anubhaw on 6/2/16.
 */
public interface ServiceCommandExecutorService {
  /**
   * Execute.
   *
   * @param command         the command
   * @param context         the context
   * @return the execution result
   */
  ExecutionResult execute(Command command, CommandExecutionContext context);
}
