package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("KubernetesDirect")
@TypeAlias("k8sDirectInfraYaml")
public class K8sDirectInfraYaml implements Infrastructure {
  @Builder.Default @NotNull private Type type = Type.KUBERNETES_DIRECT;
  @NotNull private K8sDirectInfraYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class K8sDirectInfraYamlSpec {
    private String connectorRef;
    private String namespace;
  }
}
