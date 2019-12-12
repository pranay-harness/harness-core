package io.harness.jobs.sg247.logs;

import static software.wings.common.VerificationConstants.getLogAnalysisStates;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.beans.Account;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.util.List;

@Slf4j
public class ServiceGuardLogAnalysisJob implements Job, Handler<Account> {
  public static final String SERVICE_GUARD_LOG_ANALYSIS_CRON = "SERVICE_GUARD_LOG_ANALYSIS_CRON";
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {}

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    logger.info("triggering all analysis for account {}", accountId);
    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    continuousVerificationService.triggerFeedbackAnalysis(accountId);

    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);
    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getLogAnalysisStates().contains(cvConfiguration.getStateType()))
        .map(cvConfiguration -> (LogsCVConfiguration) cvConfiguration)
        .filter(logsCVConfiguration -> logsCVConfiguration.is247LogsV2())
        .forEach(logCVConfiguration -> continuousVerificationService.trigger247LogDataV2Analysis(logCVConfiguration));
  }
}
