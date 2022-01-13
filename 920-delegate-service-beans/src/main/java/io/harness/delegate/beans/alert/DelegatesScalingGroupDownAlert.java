/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.alert;

import static java.lang.String.format;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@TargetModule(HarnessModule._955_ALERT_BEANS)
@OwnedBy(HarnessTeam.DEL)
public class DelegatesScalingGroupDownAlert implements AlertData {
  private String groupName;
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    DelegatesScalingGroupDownAlert delegatesDownAlert = (DelegatesScalingGroupDownAlert) alertData;
    return StringUtils.equals(accountId, delegatesDownAlert.getAccountId())
        && StringUtils.equals(groupName, delegatesDownAlert.getGroupName());
  }

  @Override
  public String buildTitle() {
    return format("Delegate group %s is down", groupName);
  }
}
