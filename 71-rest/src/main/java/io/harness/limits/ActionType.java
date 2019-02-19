package io.harness.limits;

import io.harness.limits.lib.LimitType;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public enum ActionType {
  CREATE_APPLICATION(Collections.singletonList(LimitType.STATIC)),
  CREATE_SERVICE(Collections.singletonList(LimitType.STATIC)),
  CREATE_USER(Collections.singletonList(LimitType.STATIC)),
  CREATE_PIPELINE(Collections.singletonList(LimitType.STATIC)),
  CREATE_WORKFLOW(Collections.singletonList(LimitType.STATIC)),
  CREATE_INFRA_PROVISIONER(Collections.singletonList(LimitType.STATIC)),
  DEPLOY(Collections.singletonList(LimitType.RATE_LIMIT));

  @Getter private List<LimitType> allowedLimitTypes;

  /**
   * @param allowedLimitTypes - the types of limits that can be applied on this action.
   * In most cases, this should be a list with one item. That is you'll either have {@link LimitType#RATE_LIMIT}
   * on an action, or {@link LimitType#STATIC} but there can be cases in which you might want to impose both types of
   * limit on action.
   */
  ActionType(List<LimitType> allowedLimitTypes) {
    this.allowedLimitTypes = allowedLimitTypes;
  }
}
