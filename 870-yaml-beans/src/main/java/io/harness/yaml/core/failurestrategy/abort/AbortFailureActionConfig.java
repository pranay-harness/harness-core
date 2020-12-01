package io.harness.yaml.core.failurestrategy.abort;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGFailureActionTypeConstants.ABORT)
public class AbortFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.ABORT;
}
