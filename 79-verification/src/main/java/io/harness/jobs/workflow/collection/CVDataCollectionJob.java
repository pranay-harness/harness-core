package io.harness.jobs.workflow.collection;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
@Slf4j
public class CVDataCollectionJob implements Job {
  @Inject private ContinuousVerificationService continuousVerificationService;
  public static final String CV_TASK_CRON = "CV_TASK_CRON";
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Triggering Data collection job");

    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    Preconditions.checkState(isNotEmpty(accountId), "account Id not found for " + jobExecutionContext);

    logger.info("starting processing cv task for account id {}", accountId);
    continuousVerificationService.processNextCVTasks(accountId);
    continuousVerificationService.expireLongRunningCVTasks(accountId);
    continuousVerificationService.retryCVTasks(accountId);
  }
}
