package io.harness.yaml.core.failurestrategy.retry;

import static io.harness.beans.rollback.NGFailureActionTypeConstants.RETRY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class RetryFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = RETRY) NGFailureActionType type = NGFailureActionType.RETRY;

  @NotNull @JsonProperty("spec") RetryFailureSpecConfig specConfig;
}
