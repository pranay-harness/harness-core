package io.harness.yaml.core.failurestrategy;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PIPELINE)
public class OnFailureConfig {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) List<NGFailureType> errors;
  @NotNull FailureStrategyActionConfig action;
}
