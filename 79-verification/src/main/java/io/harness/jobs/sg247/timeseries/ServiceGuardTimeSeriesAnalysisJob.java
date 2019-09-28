package io.harness.jobs.sg247.timeseries;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.FeatureName.MOVE_VERIFICATION_CRONS_TO_EXECUTORS;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@Slf4j
public class ServiceGuardTimeSeriesAnalysisJob implements Job {
  public static final String SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON = "SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON";

  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private VerificationManagerClient verificationManagerClient;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    Preconditions.checkState(isNotEmpty(accountId), "account Id not found for " + jobExecutionContext);
    if (!verificationManagerClientHelper
             .callManagerWithRetry(
                 verificationManagerClient.isFeatureEnabled(MOVE_VERIFICATION_CRONS_TO_EXECUTORS, accountId))
             .getResource()) {
      logger.info("Executing APM data analysis Job for {}", accountId);
      continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    } else {
      logger.info("for {} cron is disabled and data will be collected through ", accountId);
    }
  }
}
