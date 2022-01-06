/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Running this migration creates default entry in the login settings table for each account.
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddLoginSettingsToAccountMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;
  @Inject LoginSettingsService loginSettingsService;

  @Override
  public void migrate() {
    log.info("Starting AddLoginSettingsToAccountMigration migration for all accounts.");
    try (HIterator<Account> accountIterator =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accountIterator.hasNext()) {
        Account account = accountIterator.next();
        try {
          HIterator<LoginSettings> loginSettingsHIterator =
              new HIterator<>(wingsPersistence.createQuery(LoginSettings.class, excludeAuthority)
                                  .field("accountId")
                                  .equal(account.getUuid())
                                  .fetch());

          // There can be only one settings for an account.
          if (loginSettingsHIterator.hasNext()) {
            log.info("Login settings already exist for account: {}. Skipping it.", account.getUuid());
            continue;
          }
          loginSettingsService.createDefaultLoginSettings(account);
        } catch (Exception exceptionInWhileLoop) {
          log.error("Login settings migration failed for account id: {}", account.getUuid(), exceptionInWhileLoop);
        }
      }
    } catch (Exception ex) {
      log.error("AddLoginSettingsToAccountMigration migration failed.", ex);
    }
  }
}
