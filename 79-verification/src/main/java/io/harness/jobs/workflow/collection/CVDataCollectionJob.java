package io.harness.jobs.workflow.collection;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;

@Slf4j
public class CVDataCollectionJob implements Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    logger.debug("starting processing cv task for account id {}", accountId);
    long startTime = System.currentTimeMillis();
    continuousVerificationService.processNextCVTasks(accountId);
    logger.info("[processNextCVTasks] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.expireLongRunningCVTasks(accountId);
    logger.info("[expireLongRunningCVTasks] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.retryCVTasks(accountId);
    logger.info("[retryCVTasks] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
  }
}
