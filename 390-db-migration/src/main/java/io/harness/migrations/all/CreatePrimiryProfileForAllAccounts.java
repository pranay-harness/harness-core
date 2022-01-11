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
import io.harness.persistence.HKeyIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class CreatePrimiryProfileForAllAccounts implements Migration {
  @Inject private DelegateProfileService delegateProfileService;
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    log.info("Starting Migration");
    try (HKeyIterator<Account> keys = new HKeyIterator(wingsPersistence.createQuery(Account.class).fetchKeys())) {
      while (keys.hasNext()) {
        String accountId = keys.next().getId().toString();
        delegateProfileService.fetchCgPrimaryProfile(accountId);
      }
    }
  }
}
