package software.wings.yaml.command;

import software.wings.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.List;

public class ServiceCommandYaml extends BaseYaml {
  public String name;
  public String commandUnitType;
  public String commandType;
  public List<CommandUnitYaml> commandUnits = new ArrayList<>();

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

  public List<CommandUnitYaml> getCommandUnits() {
    return commandUnits;
  }

  public void setCommandUnits(List<CommandUnitYaml> commandUnits) {
    this.commandUnits = commandUnits;
  }
}
