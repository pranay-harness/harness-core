package software.wings.beans.command;

import java.io.File;

/**
 * Created by peeyushaggarwal on 7/26/16.
 */
public class CleanupCommandUnit extends AbstractCommandUnit {
  /**
   * The constant CLEANUP_UNIT.
   */
  public static final String CLEANUP_UNIT = "Cleanup";

  /**
   * Instantiates a new Init command unit.
   */
  public CleanupCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(CLEANUP_UNIT);
  }

  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    return context.executeCommandString("rm -rf " + new File("/tmp", context.getActivityId()).getAbsolutePath());
  }
}
