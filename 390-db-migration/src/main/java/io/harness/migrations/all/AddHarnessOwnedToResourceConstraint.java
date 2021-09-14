/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import io.harness.beans.ResourceConstraint;
import io.harness.beans.ResourceConstraint.ResourceConstraintKeys;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.common.InfrastructureConstants;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddHarnessOwnedToResourceConstraint implements Migration {
  private static final String DEBUG_LINE = "[Resource Constraint Migration]: ";

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info(String.join(DEBUG_LINE, " Starting Migration For Disable Assertion"));
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        log.info(
            String.join(DEBUG_LINE, " Starting Migration For Disable Assertion for account", account.getAccountName()));
        migrateResourceConstraintForAccount(account);
      }
    } catch (Exception ex) {
      log.info(String.join(DEBUG_LINE, " Exception while fetching Accounts"));
    }
  }

  private void migrateResourceConstraintForAccount(Account account) {
    try (HIterator<ResourceConstraint> resourceConstraints =
             new HIterator<>(wingsPersistence.createQuery(ResourceConstraint.class)
                                 .filter(ResourceConstraintKeys.accountId, account.getUuid())
                                 .filter(ResourceConstraintKeys.name, InfrastructureConstants.QUEUING_RC_NAME)
                                 .fetch())) {
      log.info(String.join(DEBUG_LINE, " Fetching ResourceConstraint for account", account.getAccountName(), "with Id",
          account.getUuid()));
      while (resourceConstraints.hasNext()) {
        ResourceConstraint resourceConstraint = resourceConstraints.next();
        resourceConstraint.setHarnessOwned(true);
        wingsPersistence.save(resourceConstraint);
      }
    } catch (Exception ex) {
      log.info(
          String.join(DEBUG_LINE, " Exception while fetching ResourceConstraints with Account ", account.getUuid()));
    }
  }
}
