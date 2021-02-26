package io.harness.yaml.core.failurestrategy.manualintervention;

import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.MANUAL_INTERVENTION;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualInterventionFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = MANUAL_INTERVENTION)
  NGFailureActionType type = NGFailureActionType.MANUAL_INTERVENTION;

  @NotNull @JsonProperty("spec") ManualFailureSpecConfig specConfig;
}
