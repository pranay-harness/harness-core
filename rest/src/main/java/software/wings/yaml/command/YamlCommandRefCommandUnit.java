package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class YamlCommandRefCommandUnit extends YamlCommandUnit {
  @YamlSerialize public String referenceId;
  @YamlSerialize public String commandType;

  public YamlCommandRefCommandUnit() {
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
