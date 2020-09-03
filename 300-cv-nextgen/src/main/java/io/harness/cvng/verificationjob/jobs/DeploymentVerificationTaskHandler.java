package io.harness.cvng.verificationjob.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;
import io.harness.cvng.verificationjob.services.api.DeploymentVerificationTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DeploymentVerificationTaskHandler implements Handler<DeploymentVerificationTask> {
  @Inject private DeploymentVerificationTaskService deploymentVerificationTaskService;
  @Override
  public void handle(DeploymentVerificationTask entity) {
    deploymentVerificationTaskService.createDataCollectionTasks(entity);
  }
}
