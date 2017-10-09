package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class SetupEnvCommandUnitYaml extends ExecCommandUnitYaml {
  @YamlSerialize public String commandString;

  public SetupEnvCommandUnitYaml() {
    super();
  }

  public String getCommandString() {
    return commandString;
  }

  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }
}
