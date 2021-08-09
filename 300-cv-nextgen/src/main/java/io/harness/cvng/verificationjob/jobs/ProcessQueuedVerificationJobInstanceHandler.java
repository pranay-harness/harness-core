package io.harness.cvng.verificationjob.jobs;

import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ProcessQueuedVerificationJobInstanceHandler implements Handler<VerificationJobInstance> {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Override
  public void handle(VerificationJobInstance entity) {
    verificationJobInstanceService.processVerificationJobInstance(entity);
  }
}
