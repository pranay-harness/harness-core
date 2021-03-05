package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerResizeCommandUnitYaml extends AbstractCommandUnitYaml {
  public ContainerResizeCommandUnitYaml(String commandUnitType) {
    super(commandUnitType);
  }

  public ContainerResizeCommandUnitYaml(String name, String commandUnitType, String deploymentType) {
    super(name, commandUnitType, deploymentType);
  }
}
