package software.wings.service.intfc;

import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Host;

// TODO: Auto-generated Javadoc

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
}
