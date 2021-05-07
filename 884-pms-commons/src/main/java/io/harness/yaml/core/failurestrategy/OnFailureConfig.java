package io.harness.yaml.core.failurestrategy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.common.SwaggerConstants;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class OnFailureConfig {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) List<NGFailureType> errors;
  @NotNull FailureStrategyActionConfig action;
}
