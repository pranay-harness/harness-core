package io.harness.plancreator.execution;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("executionElementConfig")
public class ExecutionElementConfig {
  String uuid;
  @NotEmpty List<ExecutionWrapperConfig> steps;
  @ApiModelProperty(hidden = true) List<ExecutionWrapperConfig> rollbackSteps;
}
