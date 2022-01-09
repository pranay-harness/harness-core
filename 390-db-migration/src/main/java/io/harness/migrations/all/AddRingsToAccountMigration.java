/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.utils.DelegateRingConstants;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class AddRingsToAccountMigration implements Migration {
  @Inject private HPersistence persistence;

  @Override
  public void migrate() {
    log.info("Starting the migration for adding ringName in account collection.");
    List<String> idsToUpdate = new ArrayList<>();

    int updated = 0;
    try (HIterator<Account> iterator = new HIterator<>(
             persistence.createQuery(Account.class).field(AccountKeys.ringName).doesNotExist().fetch())) {
      while (iterator.hasNext()) {
        idsToUpdate.add(iterator.next().getUuid());

        updated++;
        if (updated != 0 && idsToUpdate.size() % 500 == 0) {
          try {
            persistence.update(persistence.createQuery(Account.class).field(AccountKeys.uuid).in(idsToUpdate),
                persistence.createUpdateOperations(Account.class)
                    .set(AccountKeys.ringName, DelegateRingConstants.DEFAULT_RING_NAME));
          } catch (Exception e) {
            log.error("Exception occurred during add ringName to account migration.", e);
          }
          log.info("updated: " + updated);
          idsToUpdate.clear();
        }
      }

      if (!idsToUpdate.isEmpty()) {
        persistence.update(persistence.createQuery(Account.class).field(AccountKeys.uuid).in(idsToUpdate),
            persistence.createUpdateOperations(Account.class)
                .set(AccountKeys.ringName, DelegateRingConstants.DEFAULT_RING_NAME));

        log.info("updated: " + updated);
        idsToUpdate.clear();
      }
    }
    log.info("Migration complete for adding ringName to account details. Updated " + updated + " records.");
  }
}
