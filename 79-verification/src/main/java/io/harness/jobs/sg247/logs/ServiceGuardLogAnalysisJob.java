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
    logger.debug("triggering all analysis for account {}", accountId);
    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
  }
}
