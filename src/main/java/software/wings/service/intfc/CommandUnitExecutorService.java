package software.wings.service.intfc;

import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.beans.Host;

/**
 * The Interface CommandUnitExecutorService.
 */
public interface CommandUnitExecutorService {
  /**
   * Execute.
   *
   * @param host        the host
   * @param commandUnit the command unit
   * @param activityId  the activity id
   * @return the execution result
   */
  ExecutionResult execute(Host host, CommandUnit commandUnit, String activityId);

  /**
   * Clenup any resource blocked execution optimization
   *
   * @param activityId the activity id
   * @param host       the host
   */
  void cleanup(String activityId, Host host);
}
