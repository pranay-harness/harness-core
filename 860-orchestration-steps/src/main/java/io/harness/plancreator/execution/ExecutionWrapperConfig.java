package io.harness.plancreator.execution;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("executionWrapperConfig")
public class ExecutionWrapperConfig {
  @ApiModelProperty(dataType = "io.harness.yaml.core.StepElement") JsonNode step;
  @ApiModelProperty(dataType = "io.harness.plancreator.steps.ParallelStepElementConfig") JsonNode parallel;
  @ApiModelProperty(dataType = "io.harness.plancreator.steps.StepGroupElementConfig") JsonNode stepGroup;
}
