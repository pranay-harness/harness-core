/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.jobs;

import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AnalysisOrchestrationJob implements Handler<AnalysisOrchestrator> {
  @Inject private OrchestrationService orchestrationService;
  @Override
  public void handle(AnalysisOrchestrator entity) {
    orchestrationService.orchestrate(entity);
  }
}
