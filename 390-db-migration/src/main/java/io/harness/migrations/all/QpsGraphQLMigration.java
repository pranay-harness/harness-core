/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.migrations.Migration;

import software.wings.beans.Account;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class QpsGraphQLMigration implements Migration {
  @Inject LimitConfigurationService limitConfigurationService;

  private static final int globalRateLimit = 50;

  @Override
  public void migrate() {
    try {
      limitConfigurationService.configure(
          Account.GLOBAL_ACCOUNT_ID, ActionType.MAX_QPM_PER_MANAGER, new StaticLimit(globalRateLimit));
    } catch (Exception ex) {
      log.error("Exception while setting rate limit value during migration ", ex);
    }
  }
}
