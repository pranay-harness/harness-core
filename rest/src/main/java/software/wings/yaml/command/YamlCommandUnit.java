package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class YamlCommandUnit {
  @YamlSerialize public String name;
  @YamlSerialize public String commandUnitType;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCommandUnitType() {
    return commandUnitType;
  }

  public void setCommandUnitType(String commandUnitType) {
    this.commandUnitType = commandUnitType;
  }
}
