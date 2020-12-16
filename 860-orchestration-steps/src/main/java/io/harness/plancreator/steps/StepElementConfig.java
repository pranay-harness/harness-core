package io.harness.plancreator.steps;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.StepSpecType;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@TypeAlias("stepElementConfig")
public class StepElementConfig {
  String uuid;
  @EntityIdentifier String identifier;
  @EntityName String name;
  List<FailureStrategyConfig> failureStrategies;

  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  StepSpecType stepSpecType;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;
}
