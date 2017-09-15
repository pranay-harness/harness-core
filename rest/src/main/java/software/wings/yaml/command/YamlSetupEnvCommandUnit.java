package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class YamlSetupEnvCommandUnit extends YamlCommandUnit {
  @YamlSerialize public String commandString;

  public YamlSetupEnvCommandUnit() {
    super();
  }

  public String getCommandString() {
    return commandString;
  }

  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }
}
