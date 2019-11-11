package io.harness.jobs.workflow.collection;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import software.wings.beans.Account;

@Slf4j
public class CVDataCollectionJob implements Job, Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;

  public static final String CV_TASK_CRON = "CV_TASK_CRON";
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {}

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    logger.info("starting processing cv task for account id {}", accountId);
    continuousVerificationService.processNextCVTasks(accountId);
    continuousVerificationService.expireLongRunningCVTasks(accountId);
    continuousVerificationService.retryCVTasks(accountId);
  }
}
