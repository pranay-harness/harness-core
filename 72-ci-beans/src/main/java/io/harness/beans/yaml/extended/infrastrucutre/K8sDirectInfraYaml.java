package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.intfc.Infrastructure;
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
  private String type;
  private Spec spec;

  @Override
  public String getPreviousStageIdentifier() {
    return null; // TODO: this is to unblock broken master. implement the right method
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Spec {
    private String k8sConnector;
    private String namespace;
  }
}
