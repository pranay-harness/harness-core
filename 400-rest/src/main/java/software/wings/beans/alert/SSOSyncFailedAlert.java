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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Alert for displaying issues with syncing user groups with SSO provider via cron job
 *
 * @author Swapnil on 29/08/18
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SSOSyncFailedAlert implements AlertData {
  @NotNull String accountId;
  @NotNull String ssoId;
  @NotNull String message;

  @Override
  public boolean matches(AlertData alertData) {
    SSOSyncFailedAlert alert = (SSOSyncFailedAlert) alertData;
    return alert.getAccountId().equals(accountId) && alert.getSsoId().equals(ssoId);
  }

  @Override
  public String buildTitle() {
    return isEmpty(message) ? "" : message;
  }
}
