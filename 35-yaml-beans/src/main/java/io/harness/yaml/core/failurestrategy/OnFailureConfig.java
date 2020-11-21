package io.harness.yaml.core.failurestrategy;

import io.harness.common.SwaggerConstants;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnFailureConfig {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) List<NGFailureType> errors;
  @NotNull FailureStrategyActionConfig action;
}
