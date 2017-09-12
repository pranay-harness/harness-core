package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class YamlExecCommandUnit extends YamlCommandUnit {
  @YamlSerialize public String commandPath;
  @YamlSerialize public String commandString;

  public YamlExecCommandUnit() {
    super();
  }

  public String getCommandPath() {
    return commandPath;
  }

  public void setCommandPath(String commandPath) {
    this.commandPath = commandPath;
  }

  public String getCommandString() {
    return commandString;
  }

  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }
}
