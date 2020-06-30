package io.harness.cvng.statemachine.jobs;

import com.google.inject.Inject;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

public class AnalysisOrchestrationJob implements Handler<CVConfig> {
  @Inject private OrchestrationService orchestrationService;

  @Override
  public void handle(CVConfig entity) {
    orchestrationService.orchestrate(entity.getUuid());
  }
}
