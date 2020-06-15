package io.harness.jobs.sg247.logs;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.service.intfc.verification.CVConfigurationService;

@Slf4j
public class ServiceGuardLogAnalysisJob implements Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    logger.info("triggering all analysis for account {}", accountId);
    long startTime = System.currentTimeMillis();
    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    logger.info("[triggerServiceGuardTimeSeriesAnalysis] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    logger.info("[triggerLogsL1Clustering] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    logger.info("[triggerLogsL2Clustering] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    logger.info("[triggerLogDataAnalysis] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    logger.info("[triggerFeedbackAnalysis] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
  }
}
