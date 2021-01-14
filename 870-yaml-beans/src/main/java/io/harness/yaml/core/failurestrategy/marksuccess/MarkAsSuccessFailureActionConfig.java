package io.harness.yaml.core.failurestrategy.marksuccess;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarkAsSuccessFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.MARK_AS_SUCCESS;
}
