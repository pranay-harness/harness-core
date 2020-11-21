package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("kubernetes-gcp")
public class K8sGCPInfraYaml implements Infrastructure {
  @Builder.Default private Type type = Type.KUBERNETES_GCP;
  private Spec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Spec {
    @NotNull private String connectorRef;
    @NotNull private String clusterName;
    @NotNull private String namespace;
  }
}
