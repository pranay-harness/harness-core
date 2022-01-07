/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import com.amazonaws.util.StringUtils;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigrateDelegateScopes implements Migration {
  @Inject private MigrateDelegateScopesToInfraDefinition migrateDelegateScopesToInfraDefinition;
  @Inject private AccountService accountService;

  private static final String DEBUG_LINE = "DELEGATE_SCOPING_MIGRATION";
  private final String accountId = "zEaak-FLS425IEO7OLzMUg";

  @Override
  public void migrate() {
    log.info(StringUtils.join(DEBUG_LINE, "Starting Infra Definition migration for accountId ", accountId));
    Account account = accountService.get(accountId);
    if (account == null) {
      log.info(StringUtils.join(DEBUG_LINE, "Account does not exist, accountId ", accountId));
      return;
    }
    migrateDelegateScopesToInfraDefinition.migrate(account);
  }
}
