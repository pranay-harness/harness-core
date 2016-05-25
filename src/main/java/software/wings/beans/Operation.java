package software.wings.beans;

import java.util.List;

public class Operation extends Execution {
  private String customArg;

  public String getCustomArg() {
    return customArg;
  }

  public void setCustomArg(String customArg) {
    this.customArg = customArg;
  }

  @Override
  public String getCommand() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<CommandUnit> getCommandUnits() {
    return null;
  }

  @Override
  public String getSetupCommand() {
    return null;
  }

  @Override
  public String getDeployCommand() {
    return null;
  }
}
