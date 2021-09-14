/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.alert.AlertData;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * When an account is close to it's usage limit, these alerts are triggered.
 */
@Value
@AllArgsConstructor
public class InstanceUsageLimitAlert implements AlertData {
  @NotNull String accountId;
  @NotNull long usagePercentage; // this is if we want different alerts for different usage percentage
  @NotNull String message;

  @Override
  public boolean matches(AlertData alertData) {
    InstanceUsageLimitAlert alert = (InstanceUsageLimitAlert) alertData;
    return alert.getAccountId().equals(accountId) && alert.getUsagePercentage() == usagePercentage;
  }

  @Override
  public String buildTitle() {
    return isEmpty(message) ? "" : message;
  }
}
