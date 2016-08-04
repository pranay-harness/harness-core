package software.wings.beans.command;

/**
 * Created by peeyushaggarwal on 8/3/16.
 */
public class PortCheckCommandUnit extends ExecCommandUnit {
  public PortCheckCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.PROCESS_CHECK);
  }
}
