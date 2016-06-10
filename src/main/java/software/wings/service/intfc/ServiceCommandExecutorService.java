package software.wings.service.intfc;

import software.wings.beans.Command;
import software.wings.beans.CommandExecutionContext;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.ServiceInstance;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 6/2/16.
 */
public interface ServiceCommandExecutorService {
  /**
   * Execute.
   *
   * @param serviceInstance the service instance
   * @param command         the command
   * @return the execution result
   */
  ExecutionResult execute(ServiceInstance serviceInstance, Command command, CommandExecutionContext context);
}
