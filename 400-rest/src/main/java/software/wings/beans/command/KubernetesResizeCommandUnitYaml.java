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
@JsonTypeName("RESIZE_KUBERNETES")
public class KubernetesResizeCommandUnitYaml extends CommandExecutionStatusYaml {
  public KubernetesResizeCommandUnitYaml() {
    super(CommandUnitType.RESIZE_KUBERNETES.name());
  }

  @Builder
  public KubernetesResizeCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.RESIZE_KUBERNETES.name(), deploymentType);
  }
}
