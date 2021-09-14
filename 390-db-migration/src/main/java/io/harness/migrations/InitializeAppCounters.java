/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Datastore;

/**
 * Populate `limitCounters` collection with current value of applications an account has.
 */
@Slf4j
public class InitializeAppCounters implements Migration {
  @Inject AccountService accountService;
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Initializing Counters");
    Datastore ds = wingsPersistence.getDatastore(Counter.class);

    try {
      List<Account> accounts = accountService.listAllAccounts();
      ds.delete(ds.createQuery(Counter.class));

      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (accountId.equals(GLOBAL_ACCOUNT_ID)) {
          continue;
        }

        Action action = new Action(accountId, ActionType.CREATE_APPLICATION);
        long appCount =
            ds.getCount(wingsPersistence.createQuery(Application.class).field("accountId").equal(accountId));

        log.info("Initializing Counter. Account Id: {} , AppCount: {}", accountId, appCount);
        Counter counter = new Counter(action.key(), appCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      log.error("Error initializing app counters", e);
    }
  }
}
