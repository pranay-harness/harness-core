/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.limits;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.lib.LimitType;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

@OwnedBy(HarnessTeam.PL)
public enum ActionType {
  CREATE_APPLICATION(Collections.singletonList(LimitType.STATIC)),
  CREATE_SERVICE(Collections.singletonList(LimitType.STATIC)),
  CREATE_USER(Collections.singletonList(LimitType.STATIC)),
  CREATE_PIPELINE(Collections.singletonList(LimitType.STATIC)),
  CREATE_WORKFLOW(Collections.singletonList(LimitType.STATIC)),
  CREATE_INFRA_PROVISIONER(Collections.singletonList(LimitType.STATIC)),
  EXPORT_EXECUTIONS_REQUEST(Collections.singletonList(LimitType.STATIC)),
  MAX_QPM_PER_MANAGER(Collections.singletonList(LimitType.STATIC)),
  INSTANCE_USAGE_LIMIT_EXCEEDED(Collections.emptyList()),
  GRAPHQL_CALL(Collections.singletonList(LimitType.RATE_LIMIT)),
  GRAPHQL_CUSTOM_DASH_CALL(Collections.singletonList(LimitType.RATE_LIMIT)),
  DEPLOY(Collections.singletonList(LimitType.RATE_LIMIT)),
  DELEGATE_ACQUIRE_TASK(Collections.singletonList(LimitType.RATE_LIMIT)),
  LOGIN_REQUEST_TASK(Collections.singletonList(LimitType.RATE_LIMIT));

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
