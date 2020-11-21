package io.harness.cvng.statemachine.jobs;

import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;

public class DeploymentVerificationJobInstanceOrchestrationJob implements Handler<VerificationJobInstance> {
  @Inject private OrchestrationService orchestrationService;
  @Inject private VerificationTaskService verificationTaskService;
  @Override
  public void handle(VerificationJobInstance entity) {
    verificationTaskService.getVerificationTaskIds(entity.getAccountId(), entity.getUuid())
        .forEach(orchestrationService::orchestrate);
  }
}
