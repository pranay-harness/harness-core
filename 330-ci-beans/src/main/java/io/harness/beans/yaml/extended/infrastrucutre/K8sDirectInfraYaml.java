package io.harness.beans.yaml.extended.infrastrucutre;

import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
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
    private ParameterField<Map<String, String>> annotations;
    private ParameterField<Map<String, String>> labels;
    @JsonIgnore @ApiModelProperty(hidden = true) private ParameterField<Integer> runAsUser;
  }
}
