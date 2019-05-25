package io.harness.jobs.sg247.collection;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * This single cron collects data for both timeseries and log for
 * service guard 24X7 analysis for a specific account.
 */
@Slf4j
public class ServiceGuardDataCollectionJob implements Job {
  public static final String SERVICE_GUARD_DATA_COLLECTION_CRON = "SERVICE_GUARD_DATA_COLLECTION_CRON";

  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    Preconditions.checkState(isNotEmpty(accountId), "account Id not found for " + jobExecutionContext);

    logger.info("Executing APM & Logs Data collector Job for {}", accountId);
    continuousVerificationService.triggerAPMDataCollection(accountId);
    continuousVerificationService.triggerLogDataCollection(accountId);
  }
}
