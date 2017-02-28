package software.wings.beans.command;

import java.util.List;

/**
 * Created by anubhaw on 2/28/17.
 */
public class ResizeCommandUnitExecutionData extends CommandExecutionData {
  private List<String> containerIds;

  public List<String> getContainerIds() {
    return containerIds;
  }

  public void setContainerIds(List<String> containerIds) {
    this.containerIds = containerIds;
  }
}
