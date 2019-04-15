package io.harness.jobs;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 *  Handles scheduling jobs related to APM
 * Created by Pranjal on 10/04/2018
 */
@Slf4j
public class MetricDataProcessorJob implements Job {
  public static final String METRIC_DATA_PROCESSOR_CRON_GROUP = "METRIC_DATA_PROCESSOR_CRON_GROUP";

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
