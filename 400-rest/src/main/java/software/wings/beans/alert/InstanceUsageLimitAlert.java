/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
