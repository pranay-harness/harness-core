/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Vaibhav Tulsyan
 * 12/Jun/2019
 */
@Slf4j
@Singleton
public class OauthEnabledFieldMigration implements Migration {
  private WingsPersistence wingsPersistence;

  @Inject
  public OauthEnabledFieldMigration(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    log.info("Starting iterating through all accounts ...");
    try (HIterator<Account> accountHIterator =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accountHIterator.hasNext()) {
        Account account = accountHIterator.next();
        AuthenticationMechanism authenticationMechanism = account.getAuthenticationMechanism();
        log.info("Processing accountId {}", account.getUuid());
        try {
          boolean oauthEnabled = AuthenticationMechanism.OAUTH == authenticationMechanism;
          account.setOauthEnabled(oauthEnabled);
          wingsPersistence.save(account);
          log.info("Successfully set oauthEnabled={} for accountId {}", oauthEnabled, account.getUuid());
        } catch (Exception e) {
          log.error(
              "Failed to set oauthEnabled field as true for accountId {} with exception {}.", account.getUuid(), e);
        }
      }
    } catch (Exception e) {
      log.error("Failure occurred in OauthEnabledFieldMigration with exception {}", e);
    }
    log.info("OauthEnabledFieldMigration has completed.");
  }
}
