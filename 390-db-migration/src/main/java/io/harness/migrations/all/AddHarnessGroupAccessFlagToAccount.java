/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.Account.AccountKeys;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddHarnessGroupAccessFlagToAccount implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Starting migration for adding isHarnessSupportAccessAllowed flag to accounts");

    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority)
                                                           .project(AccountKeys.isHarnessSupportAccessAllowed, true)
                                                           .fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        wingsPersistence.updateField(
            Account.class, account.getUuid(), AccountKeys.isHarnessSupportAccessAllowed, Boolean.TRUE);
      }
    } catch (Exception e) {
      log.error("Error happened while adding isHarnessSupportAccessAllowed flag to accounts", e);
    }
  }
}
