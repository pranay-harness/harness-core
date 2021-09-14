/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import io.harness.alert.AlertData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvalidSMTPConfigAlert implements AlertData {
  String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    return ((InvalidSMTPConfigAlert) alertData).accountId.equals(accountId);
  }

  @Override
  public String buildTitle() {
    return "No Valid SMTP configuration available";
  }
}
