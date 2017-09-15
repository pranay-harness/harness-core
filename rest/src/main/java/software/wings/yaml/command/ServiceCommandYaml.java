package software.wings.yaml.command;

import software.wings.yaml.GenericYaml;
import software.wings.yaml.YamlSerialize;

import java.util.ArrayList;
import java.util.List;

public class ServiceCommandYaml extends GenericYaml {
  @YamlSerialize public String name;
  @YamlSerialize public String commandUnitType;
  @YamlSerialize public String commandType;
  @YamlSerialize public List<YamlCommandUnit> commandUnits = new ArrayList<>();

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

  public String getCommandType() {
    return commandType;
  }

  public void setCommandType(String commandType) {
    this.commandType = commandType;
  }

  public List<YamlCommandUnit> getCommandUnits() {
    return commandUnits;
  }

  public void setCommandUnits(List<YamlCommandUnit> commandUnits) {
    this.commandUnits = commandUnits;
  }
}
