package software.wings.service.intfc;

import software.wings.beans.ServiceInstance;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit.ExecutionResult;

/**
 * Created by anubhaw on 6/2/16.
 */
public interface ServiceCommandExecutorService {
  /**
   * Execute.
   *
   * @param serviceInstance the service instance
   * @param command         the command
   * @param context         the context
   * @return the execution result
   */
  ExecutionResult execute(ServiceInstance serviceInstance, Command command, CommandExecutionContext context);
}
