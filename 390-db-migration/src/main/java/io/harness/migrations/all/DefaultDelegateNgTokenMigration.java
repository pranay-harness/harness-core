/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;
import io.harness.service.intfc.DelegateNgTokenService;

import software.wings.beans.Account;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DefaultDelegateNgTokenMigration implements Migration {
  @Inject private HPersistence persistence;
  @Inject private DelegateNgTokenService delegateNgTokenService;

  @Override
  public void migrate() {
    generateDefaultDelegateTokenForAccounts();
  }

  private void generateDefaultDelegateTokenForAccounts() {
    log.info("Generating default tokens for accounts");
    Query<Account> accountsQuery = persistence.createQuery(Account.class, HQuery.excludeAuthority);
    try (HIterator<Account> accounts = new HIterator<>(accountsQuery.fetch())) {
      for (Account account : accountsQuery) {
        delegateNgTokenService.upsertDefaultToken(account.getUuid(), null, true);
        log.info("Successfully created default Delegate Token for account {}", account.getUuid());
      }
    }
  }
}
