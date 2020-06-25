package io.harness.jobs;

import com.google.inject.Inject;

import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.statemachine.service.intfc.OrchestrationService;

public class AnalysisOrchestrationJob implements Handler<CVConfig> {
  @Inject private OrchestrationService orchestrationService;

  @Override
  public void handle(CVConfig entity) {
    orchestrationService.orchestrate(entity.getUuid());
  }
}
