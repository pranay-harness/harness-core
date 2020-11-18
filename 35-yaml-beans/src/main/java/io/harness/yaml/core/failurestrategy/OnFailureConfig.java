package io.harness.yaml.core.failurestrategy;

import io.harness.common.SwaggerConstants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.List;

@Value
@Builder
public class OnFailureConfig {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) List<NGFailureType> errors;
  @NotNull FailureStrategyActionConfig action;
}
