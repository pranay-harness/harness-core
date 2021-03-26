package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
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
