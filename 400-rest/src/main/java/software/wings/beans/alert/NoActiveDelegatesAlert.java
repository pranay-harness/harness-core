/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class NoActiveDelegatesAlert implements AlertData {
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    return accountId.equals(((NoActiveDelegatesAlert) alertData).getAccountId());
  }

  @Override
  public String buildTitle() {
    return "No delegates are available";
  }
}
