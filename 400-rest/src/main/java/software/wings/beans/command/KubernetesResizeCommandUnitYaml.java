package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("RESIZE_KUBERNETES")
public class KubernetesResizeCommandUnitYaml extends ContainerResizeCommandUnitYaml {
  public KubernetesResizeCommandUnitYaml() {
    super(CommandUnitType.RESIZE_KUBERNETES.name());
  }

  @Builder
  public KubernetesResizeCommandUnitYaml(String name, String deploymentType) {
    super(name, CommandUnitType.RESIZE_KUBERNETES.name(), deploymentType);
  }
}
