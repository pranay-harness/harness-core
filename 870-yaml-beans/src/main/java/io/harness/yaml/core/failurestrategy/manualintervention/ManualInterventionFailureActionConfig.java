package io.harness.yaml.core.failurestrategy.manualintervention;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualInterventionFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.MANUAL_INTERVENTION;
  @JsonProperty("spec") ManualFailureSpecConfig specConfig;
}
