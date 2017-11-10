package software.wings.yaml.command;

public class CommandRefCommandUnitYaml extends CommandUnitYaml {
  public String referenceId;
  public String commandType;

  public CommandRefCommandUnitYaml() {
    super();
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public String getCommandType() {
    return commandType;
  }

  public void setCommandType(String commandType) {
    this.commandType = commandType;
  }
}
