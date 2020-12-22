package io.harness.beans.stages;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.CustomVariable;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageInfoConfig;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonTypeName("ci")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("integrationStage")
public class IntegrationStageConfig implements StageInfoConfig {
  String uuid;
  List<CustomVariable> variables;
  private ParameterField<List<String>> sharedPaths;
  ExecutionElementConfig execution;
  private ParameterField<String> workingDirectory;
  private Infrastructure infrastructure;
  private List<DependencyElement> dependencies;
  private ParameterField<Boolean> cloneRepository;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;
}
