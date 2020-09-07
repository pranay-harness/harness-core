package io.harness.cvng.verificationjob.jobs;

import com.google.inject.Inject;

import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;
import io.harness.cvng.verificationjob.services.api.DeploymentVerificationTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

public class DeletePerpetualTasksHandler implements MongoPersistenceIterator.Handler<DeploymentVerificationTask> {
  @Inject private DeploymentVerificationTaskService deploymentVerificationTaskService;
  @Override
  public void handle(DeploymentVerificationTask entity) {
    deploymentVerificationTaskService.deletePerpetualTasks(entity);
  }
}