package io.harness.plancreator.stages.stage;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@TypeAlias("stageElementConfig")
public class StageElementConfig {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String uuid;
  @NotNull @EntityIdentifier String identifier;
  @EntityName String name;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  ParameterField<String> skipCondition;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> when;

  List<FailureStrategyConfig> failureStrategies;
  List<NGVariable> variables;
  @NotNull String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  StageInfoConfig stageType;

  @Builder
  public StageElementConfig(String uuid, String identifier, String name, ParameterField<String> description,
      List<FailureStrategyConfig> failureStrategies, String type, StageInfoConfig stageType,
      ParameterField<String> skipCondition, ParameterField<String> when) {
    this.uuid = uuid;
    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.failureStrategies = failureStrategies;
    this.type = type;
    this.stageType = stageType;
    this.skipCondition = skipCondition;
    this.when = when;
  }
}
