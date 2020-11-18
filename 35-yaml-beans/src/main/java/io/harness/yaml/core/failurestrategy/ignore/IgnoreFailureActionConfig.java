package io.harness.yaml.core.failurestrategy.ignore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGFailureActionTypeConstants.IGNORE)
public class IgnoreFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.IGNORE;
}
