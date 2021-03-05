package software.wings.beans.container;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
