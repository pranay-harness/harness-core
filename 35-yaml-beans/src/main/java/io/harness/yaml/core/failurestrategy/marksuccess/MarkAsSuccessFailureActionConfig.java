package io.harness.yaml.core.failurestrategy.marksuccess;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGFailureActionTypeConstants.MARK_AS_SUCCESS)
public class MarkAsSuccessFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.MARK_AS_SUCCESS;
}
