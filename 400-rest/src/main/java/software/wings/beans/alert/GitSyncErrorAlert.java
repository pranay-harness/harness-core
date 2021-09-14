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

/**
 * @author rktummala on 12/19/2017
 */
@Data
@Builder
public class GitSyncErrorAlert implements AlertData {
  private String accountId;
  private String message;
  private boolean gitToHarness;

  @Override
  public boolean matches(AlertData alertData) {
    return accountId.equals(((GitSyncErrorAlert) alertData).accountId);
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
