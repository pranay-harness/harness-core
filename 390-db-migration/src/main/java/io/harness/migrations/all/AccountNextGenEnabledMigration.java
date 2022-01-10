/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureFlag.FeatureFlagKeys;
import io.harness.ff.FeatureFlagService;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class AccountNextGenEnabledMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void migrate() {
    FeatureFlag featureFlag =
        wingsPersistence.createQuery(FeatureFlag.class).field(FeatureFlagKeys.name).equal("NEXT_GEN_ENABLED").get();
    if (featureFlag != null) {
      try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
        for (Account account : accounts) {
          if (featureFlag.getAccountIds().contains(account.getUuid())) {
            wingsPersistence.updateField(Account.class, account.getUuid(), AccountKeys.nextGenEnabled, Boolean.TRUE);
          } else {
            wingsPersistence.updateField(Account.class, account.getUuid(), AccountKeys.nextGenEnabled, Boolean.FALSE);
          }
        }
      }
    }
  }
}
