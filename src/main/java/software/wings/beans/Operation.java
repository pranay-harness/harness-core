package software.wings.beans;

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
}
