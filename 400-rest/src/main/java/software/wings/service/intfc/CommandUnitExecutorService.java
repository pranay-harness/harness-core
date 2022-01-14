package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessModule._930_DELEGATE_TASKS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.infrastructure.Host;

/**
 * The Interface CommandUnitExecutorService.
 */
@OwnedBy(CDP)
@TargetModule(_930_DELEGATE_TASKS)
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
