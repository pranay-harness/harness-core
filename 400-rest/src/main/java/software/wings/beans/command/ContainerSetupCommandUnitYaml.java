package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerSetupCommandUnitYaml extends AbstractCommandUnitYaml {
  public ContainerSetupCommandUnitYaml(String commandUnitType) {
    super(commandUnitType);
  }

  public ContainerSetupCommandUnitYaml(String name, String commandUnitType, String deploymentType) {
    super(name, commandUnitType, deploymentType);
  }
}
