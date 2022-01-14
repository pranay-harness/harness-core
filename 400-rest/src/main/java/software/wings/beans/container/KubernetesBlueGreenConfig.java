package software.wings.beans.container;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KubernetesBlueGreenConfig {
  private KubernetesServiceSpecification primaryService;
  private KubernetesServiceSpecification stageService;
  private boolean useIngress;
  private String ingressYaml;
}
