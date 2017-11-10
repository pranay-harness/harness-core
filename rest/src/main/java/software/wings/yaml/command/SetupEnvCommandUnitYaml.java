package software.wings.yaml.command;

public class SetupEnvCommandUnitYaml extends ExecCommandUnitYaml {
  public String commandString;

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
