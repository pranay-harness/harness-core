package software.wings.service.intfc;

import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.infrastructure.Host;

/**
 * The Interface CommandUnitExecutorService.
 */
public interface CommandUnitExecutorService {
  /**
   * Execute execution result.
   *
   * @param commandUnit the command unit
   * @param context     the context
   * @return the execution result
   */
  CommandExecutionStatus execute(CommandUnit commandUnit, CommandExecutionContext context);

  /**
   * Clenup any resource blocked execution optimization
   *
   * @param activityId the activity id
   * @param host       the host
   */
  default void cleanup(String activityId, Host host) {}
}
