package software.wings.beans.command;

import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;

import software.wings.api.DeploymentType;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class SshCommandUnit extends AbstractCommandUnit {
  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public SshCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    super.setDeploymentType(DeploymentType.SSH.name());
  }

  @Override
  public final CommandExecutionStatus execute(CommandExecutionContext context) {
    if (!(context instanceof ShellCommandExecutionContext)) {
      throw new WingsException("Unexpected context type");
    }
    return executeInternal((ShellCommandExecutionContext) context);
  }

  protected abstract CommandExecutionStatus executeInternal(ShellCommandExecutionContext context);
}
