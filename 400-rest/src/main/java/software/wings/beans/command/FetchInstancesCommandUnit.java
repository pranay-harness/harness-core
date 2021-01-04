package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

public class FetchInstancesCommandUnit extends AbstractCommandUnit {
  /**
   * Instantiates a new Command unit.
   */
  public FetchInstancesCommandUnit(String name) {
    super(CommandUnitType.FETCH_INSTANCES_DUMMY);
    setName(name);
  }

  /**
   * Execute execution result.
   *
   * @param context the context
   * @return the execution result
   */
  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
