package software.wings.beans.container;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.DeploymentSpecificationYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerTaskYaml extends DeploymentSpecificationYaml {
  private String advancedConfig;
  private ContainerDefinitionYaml containerDefinition;

  protected ContainerTaskYaml(
      String type, String harnessApiVersion, String advancedConfig, ContainerDefinitionYaml containerDefinition) {
    super(type, harnessApiVersion);
    this.advancedConfig = advancedConfig;
    this.containerDefinition = containerDefinition;
  }
}
