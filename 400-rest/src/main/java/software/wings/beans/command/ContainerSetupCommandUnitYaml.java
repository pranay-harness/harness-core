package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerSetupCommandUnitYaml extends AbstractCommandUnitYaml {
  public ContainerSetupCommandUnitYaml(String commandUnitType) {
    super(commandUnitType);
  }

  public ContainerSetupCommandUnitYaml(String name, String commandUnitType, String deploymentType) {
    super(name, commandUnitType, deploymentType);
  }
}
