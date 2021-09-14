/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@TargetModule(HarnessModule._955_ALERT_BEANS)
public class PerpetualTaskAlert implements AlertData {
  private String accountId;
  private String description;
  private String perpetualTaskType;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    PerpetualTaskAlert otherAlert = (PerpetualTaskAlert) alertData;
    return StringUtils.equals(accountId, otherAlert.getAccountId())
        && StringUtils.equals(perpetualTaskType, otherAlert.getPerpetualTaskType());
  }

  @Override
  public String buildTitle() {
    if (isNotBlank(description)) {
      return String.format("%s. %s", message, description);
    }

    return message;
  }
}
