package software.wings.beans.container;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KubernetesContainerTaskYaml extends ContainerTaskYaml {
  @Builder
  public KubernetesContainerTaskYaml(
      String type, String harnessApiVersion, String advancedConfig, ContainerDefinitionYaml containerDefinition) {
    super(type, harnessApiVersion, advancedConfig, containerDefinition);
  }
}
