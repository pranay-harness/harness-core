package io.harness.plancreator.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.when.beans.StepWhenCondition;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("stepGroupElementConfig")
@OwnedBy(PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class StepGroupElementConfig {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  @NotNull @EntityIdentifier String identifier;
  @EntityName String name;

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  ParameterField<String> skipCondition;
  StepWhenCondition when;

  List<FailureStrategyConfig> failureStrategies;
  @NotNull List<ExecutionWrapperConfig> steps;
  List<ExecutionWrapperConfig> rollbackSteps;
}
