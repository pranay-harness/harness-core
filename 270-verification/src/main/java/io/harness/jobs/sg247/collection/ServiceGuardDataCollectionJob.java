/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.jobs.sg247.collection;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;

import software.wings.beans.Account;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * This single cron collects data for both timeseries and log for
 * service guard 24X7 analysis for a specific account.
 */
@Slf4j
public class ServiceGuardDataCollectionJob implements Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void handle(Account account) {
    if (!continuousVerificationService.shouldPerformServiceGuardTasks(account)) {
      return;
    }
    final String accountId = account.getUuid();
    log.info("Executing APM & Logs Data collection for {}", accountId);
    long startTime = System.currentTimeMillis();
    continuousVerificationService.triggerAPMDataCollection(accountId);
    log.info("[triggerAPMDataCollection] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogDataCollection(accountId);
    log.info("[triggerLogDataCollection] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
  }
}
