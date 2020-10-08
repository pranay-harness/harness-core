package io.harness.cvng.verificationjob.jobs;

import com.google.inject.Inject;

import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

public class DeletePerpetualTasksHandler implements MongoPersistenceIterator.Handler<VerificationJobInstance> {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Override
  public void handle(VerificationJobInstance entity) {
    verificationJobInstanceService.deletePerpetualTasks(entity);
  }
}
