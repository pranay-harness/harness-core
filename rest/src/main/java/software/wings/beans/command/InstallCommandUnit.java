package software.wings.beans.command;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class InstallCommandUnit extends ContainerOrcherstrationCommandUnit {
  public InstallCommandUnit() {
    super(CommandUnitType.INSTALL);
  }

  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    context.getServiceVariables();
    context.getServiceTemplate().getService();
    return null;
  }
}
