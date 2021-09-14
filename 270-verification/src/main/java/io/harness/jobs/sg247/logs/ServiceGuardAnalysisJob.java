/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.jobs.sg247.logs;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;

import software.wings.beans.Account;
import software.wings.service.intfc.verification.CVConfigurationService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceGuardAnalysisJob implements Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void handle(Account account) {
    if (!continuousVerificationService.shouldPerformServiceGuardTasks(account)) {
      return;
    }
    final String accountId = account.getUuid();
    log.info("triggering all analysis for account {}", accountId);
    long startTime = System.currentTimeMillis();
    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    log.info("[triggerServiceGuardTimeSeriesAnalysis] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    log.info("[triggerLogsL1Clustering] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    log.info("[triggerLogsL2Clustering] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    log.info("[triggerLogDataAnalysis] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    log.info("[triggerFeedbackAnalysis] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
  }
}
