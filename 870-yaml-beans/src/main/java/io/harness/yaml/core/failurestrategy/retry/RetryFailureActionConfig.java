package io.harness.yaml.core.failurestrategy.retry;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGFailureActionTypeConstants.RETRY)
public class RetryFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.RETRY;
  @JsonProperty("spec") RetryFailureSpecConfig specConfig;
}
