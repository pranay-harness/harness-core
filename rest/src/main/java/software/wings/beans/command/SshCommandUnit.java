package software.wings.beans.command;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.DeploymentType;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class SshCommandUnit extends CommandExecutionResult.AbstractCommandUnit {
  @SchemaIgnore private String deploymentType;

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
    return executeInternal((SshCommandExecutionContext) context);
  }

  protected abstract CommandExecutionStatus executeInternal(SshCommandExecutionContext context);
}
