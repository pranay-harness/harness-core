/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTokenService;

import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenMigration implements Migration {
  @Inject private HPersistence persistence;
  @Inject private DelegateTokenService delegateTokenService;

  @Override
  public void migrate() {
    log.info("Starting the migration of the delegates tokens for all the accounts.");

    Query<Account> query =
        persistence.createQuery(Account.class, excludeAuthority).field(AccountKeys.accountKey).exists();

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      for (Account account : records) {
        delegateTokenService.upsertDefaultToken(account.getUuid(), account.getAccountKey());
      }
    }

    log.info("The migration of the delegates tokens for all accounts has finished.");
  }
}
