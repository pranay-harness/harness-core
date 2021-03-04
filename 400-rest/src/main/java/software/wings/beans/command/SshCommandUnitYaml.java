package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SshCommandUnitYaml extends AbstractCommandUnitYaml {
  public SshCommandUnitYaml(String commandUnitType) {
    super(commandUnitType);
  }

  public SshCommandUnitYaml(String name, String commandUnitType, String deploymentType) {
    super(name, commandUnitType, deploymentType);
  }
}
