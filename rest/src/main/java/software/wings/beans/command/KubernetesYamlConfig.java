package software.wings.beans.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KubernetesYamlConfig {
  private String controllerYaml;
  private String configMapYaml;
  private String secretMapYaml;
  private String autoscalerYaml;
}
