package io.harness.beans.yaml.extended.infrastrucutre;

import static io.harness.beans.common.SwaggerConstants.INTEGER_CLASSPATH;
import static io.harness.beans.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.beans.common.SwaggerConstants.STRING_MAP_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

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
    @NotNull private String connectorRef;
    @NotNull private String namespace;
    @YamlSchemaTypes(value = {string})
    @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
    private ParameterField<Map<String, String>> annotations;
    @YamlSchemaTypes(value = {string})
    @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
    private ParameterField<Map<String, String>> labels;
    @YamlSchemaTypes({string})
    @ApiModelProperty(dataType = INTEGER_CLASSPATH)
    private ParameterField<Integer> runAsUser;
    @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> serviceAccountName;
    @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> initTimeout;
  }
}
