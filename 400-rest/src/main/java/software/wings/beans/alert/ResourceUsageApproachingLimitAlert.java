/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
