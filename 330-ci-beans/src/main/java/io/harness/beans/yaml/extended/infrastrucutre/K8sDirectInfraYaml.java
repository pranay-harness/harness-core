package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("kubernetes-direct")
public class K8sDirectInfraYaml implements Infrastructure {
  @Builder.Default private Type type = Type.KUBERNETES_DIRECT;
  private Spec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Spec {
    private String connectorRef;
    private String namespace;
  }
}
