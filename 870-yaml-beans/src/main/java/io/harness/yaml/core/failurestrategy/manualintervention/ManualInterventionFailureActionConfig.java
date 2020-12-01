package io.harness.yaml.core.failurestrategy.manualintervention;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGFailureActionTypeConstants.MANUAL_INTERVENTION)
public class ManualInterventionFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.MANUAL_INTERVENTION;
  @JsonProperty("spec") ManualFailureSpecConfig specConfig;
}
