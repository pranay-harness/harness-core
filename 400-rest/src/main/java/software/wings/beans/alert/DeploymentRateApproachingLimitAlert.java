/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
