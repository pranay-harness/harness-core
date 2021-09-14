/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.alert.AlertData;
import io.harness.limits.lib.Limit;

import lombok.Value;

@Value
public class DeploymentRateApproachingLimitAlert implements AlertData {
  private Limit limit;
  private String accountId;
  private int percent;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    DeploymentRateApproachingLimitAlert alert = (DeploymentRateApproachingLimitAlert) alertData;
    return alert.getAccountId().equals(accountId) && alert.getPercent() == percent;
  }

  @Override
  public String buildTitle() {
    return isEmpty(message) ? "" : message;
  }
}
