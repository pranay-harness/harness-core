package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KUBERNETES_SETUP")
public class KubernetesSetupCommandUnitYaml extends ContainerSetupCommandUnitYaml {
  public KubernetesSetupCommandUnitYaml() {
    super(CommandUnitType.KUBERNETES_SETUP.name());
  }

  @Builder
  public KubernetesSetupCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.KUBERNETES_SETUP.name(), deploymentType);
  }
}
