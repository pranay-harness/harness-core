package software.wings.beans.command;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.DeploymentType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

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

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  @Override
  public final CommandExecutionStatus execute(CommandExecutionContext context) {
    return executeInternal((ShellCommandExecutionContext) context);
  }

  protected abstract CommandExecutionStatus executeInternal(ShellCommandExecutionContext context);

  @Data
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml(String commandUnitType) {
      super(commandUnitType);
    }

    public Yaml(String name, String commandUnitType, String deploymentType) {
      super(name, commandUnitType, deploymentType);
    }
  }
}
