package io.harness.jobs.workflow.collection;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;

import software.wings.beans.Account;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CVDataCollectionJob implements Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    continuousVerificationService.processNextCVTasks(accountId);
    continuousVerificationService.expireLongRunningCVTasks(accountId);
    continuousVerificationService.retryCVTasks(accountId);
  }
}
