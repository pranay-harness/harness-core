package io.harness.cvng.verificationjob.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.verificationjob.entities.VerificationTask;
import io.harness.cvng.verificationjob.services.api.VerificationTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class VerificationTaskHandler implements Handler<VerificationTask> {
  @Inject private VerificationTaskService verificationTaskService;
  @Override
  public void handle(VerificationTask entity) {
    verificationTaskService.createDataCollectionTasks(entity);
  }
}
