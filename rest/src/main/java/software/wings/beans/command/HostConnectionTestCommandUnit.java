package software.wings.beans.command;

/**
 * Created by anubhaw on 2/22/17.
 */
public class HostConnectionTestCommandUnit extends SshCommandUnit {
  /**
   * Instantiates a new command unit.
   *
   * */
  public HostConnectionTestCommandUnit() {
    super(CommandUnitType.EXEC);
    setName("HOST_CONNECTION_TEST");
  }

  @Override
  protected ExecutionResult executeInternal(SshCommandExecutionContext context) {
    return null;
  }
}
