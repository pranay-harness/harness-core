package software.wings.beans.command;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
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
