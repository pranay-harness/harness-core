package io.harness.cvng.verificationjob.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CreateDeploymentDataCollectionTaskHandler implements Handler<VerificationJobInstance> {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Override
  public void handle(VerificationJobInstance entity) {
    verificationJobInstanceService.createDataCollectionTasks(entity);
  }
}
