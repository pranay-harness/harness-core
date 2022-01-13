/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.event.usagemetrics.UsageMetricsService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.workers.background.AccountLevelEntityProcessController;

import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UsageMetricsHandler implements Handler<Account> {
  @Inject private UsageMetricsService usageMetricsService;
  @Inject private AccountService accountService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Account> persistenceProvider;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("UsageMetricsHandler").poolSize(2).interval(ofSeconds(30)).build(),
        UsageMetricsHandler.class,
        MongoPersistenceIterator.<Account, MorphiaFilterExpander<Account>>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.usageMetricsTaskIteration)
            .targetInterval(ofHours(24))
            .acceptableNoAlertDelay(ofMinutes(30))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .entityProcessController(new AccountLevelEntityProcessController(accountService))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Account account) {
    if ((account.getLicenseInfo() == null
            || ((account.getLicenseInfo() != null && account.getLicenseInfo().getAccountStatus() != null)
                && (account.getLicenseInfo().getAccountStatus().equals(AccountStatus.ACTIVE)
                    && (account.getLicenseInfo().getAccountType().equals(AccountType.TRIAL)
                        || account.getLicenseInfo().getAccountType().equals(AccountType.PAID)))))
        && (!account.getUuid().equals(GLOBAL_ACCOUNT_ID))) {
      usageMetricsService.createVerificationUsageEvents(account);
      usageMetricsService.createSetupEventsForTimescaleDB(account);
    }
  }
}
