package software.wings.beans.command;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class SshCommandUnit extends AbstractCommandUnit {
  /**
   * Instantiates a new Command unit.
   */
  public SshCommandUnit() {
    super();
  }

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public SshCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }

  @Override
  public final ExecutionResult execute(CommandExecutionContext context) {
    return executeInternal((SshCommandExecutionContext) context);
  }

  protected abstract ExecutionResult executeInternal(SshCommandExecutionContext context);
}
