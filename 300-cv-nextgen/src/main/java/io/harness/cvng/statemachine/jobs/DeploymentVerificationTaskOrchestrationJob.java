package io.harness.cvng.statemachine.jobs;

import com.google.inject.Inject;

import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

public class DeploymentVerificationTaskOrchestrationJob implements Handler<DeploymentVerificationTask> {
  @Inject private OrchestrationService orchestrationService;
  @Inject private VerificationTaskService verificationTaskService;
  @Override
  public void handle(DeploymentVerificationTask entity) {
    verificationTaskService.getVerificationTaskIds(entity.getAccountId(), entity.getUuid())
        .forEach(orchestrationService::orchestrate);
  }
}
