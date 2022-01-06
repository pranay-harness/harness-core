/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.alert.AlertData;
import io.harness.limits.ActionType;
import io.harness.limits.lib.StaticLimit;

import software.wings.service.impl.instance.limits.ApproachingLimitsMessage;

import lombok.Value;

@Value
public class ResourceUsageApproachingLimitAlert implements AlertData {
  private StaticLimit limit;
  private String accountId;
  private ActionType actionType;
  private int percent;
  private String message;

  public ResourceUsageApproachingLimitAlert(StaticLimit limit, String accountId, ActionType actionType, int percent) {
    this.limit = limit;
    this.accountId = accountId;
    this.actionType = actionType;
    this.percent = percent;
    this.message = ApproachingLimitsMessage.warningMessage(percent, actionType);
  }

  @Override
  public boolean matches(AlertData alertData) {
    ResourceUsageApproachingLimitAlert alert = (ResourceUsageApproachingLimitAlert) alertData;
    return alert.getAccountId().equals(accountId) && alert.getPercent() == percent
        && alert.getActionType() == actionType && alert.getLimit().equals(limit);
  }

  @Override
  public String buildTitle() {
    return isEmpty(message) ? "" : message;
  }
}
