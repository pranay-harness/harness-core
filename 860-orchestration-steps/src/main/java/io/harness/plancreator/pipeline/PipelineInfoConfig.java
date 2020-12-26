package io.harness.plancreator.pipeline;

import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.properties.NGProperties;
import io.harness.yaml.core.variables.NGVariable;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("pipelineInfoConfig")
public class PipelineInfoConfig {
  String uuid;
  @NotNull @EntityName String name;
  @NotNull @EntityIdentifier String identifier;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;
  Map<String, String> tags;

  List<NGVariable> variables;
  NGProperties properties;

  @Singular List<StageElementWrapperConfig> stages;
}
