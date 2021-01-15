package io.harness.beans.stages;

import io.harness.EntityType;
import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageInfoConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.schema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("CI")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("integrationStage")
@YamlSchemaRoot(EntityType.INTEGRATION_STAGE)
public class IntegrationStageConfig implements StageInfoConfig {
  String uuid;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) private ParameterField<List<String>> sharedPaths;
  ExecutionElementConfig execution;
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UseFromStageInfraYaml.class)
  private Infrastructure infrastructure;
  private List<DependencyElement> serviceDependencies;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<Boolean> cloneCodebase;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;
}
