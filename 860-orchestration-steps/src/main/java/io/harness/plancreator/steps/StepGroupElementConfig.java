package io.harness.plancreator.steps;

import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("stepGroupElementConfig")
public class StepGroupElementConfig {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String uuid;
  @NotNull @EntityIdentifier String identifier;
  @EntityName String name;

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  ParameterField<String> skipCondition;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> when;

  List<FailureStrategyConfig> failureStrategies;
  @NotNull List<ExecutionWrapperConfig> steps;
  List<ExecutionWrapperConfig> rollbackSteps;
}
