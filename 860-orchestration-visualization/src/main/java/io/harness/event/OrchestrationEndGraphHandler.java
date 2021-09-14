/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.OrchestrationEndObserver;
import io.harness.execution.PlanExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class OrchestrationEndGraphHandler implements AsyncInformObserver, OrchestrationEndObserver {
  private final ExecutorService executorService;
  private final PlanExecutionService planExecutionService;
  private final GraphGenerationService graphGenerationService;

  @Inject
  public OrchestrationEndGraphHandler(
      @Named("OrchestrationVisualizationExecutorService") ExecutorService executorService,
      PlanExecutionService planExecutionService, GraphGenerationService graphGenerationService) {
    this.executorService = executorService;
    this.planExecutionService = planExecutionService;
    this.graphGenerationService = graphGenerationService;
  }

  @Override
  public void onEnd(Ambiance ambiance) {
    try {
      PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
      // One last time try to update the graph to process any unprocessed logs
      graphGenerationService.updateGraph(planExecution.getUuid());

      log.info("Ending Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());

      OrchestrationGraph orchestrationGraph =
          graphGenerationService.getCachedOrchestrationGraph(ambiance.getPlanExecutionId());
      orchestrationGraph = orchestrationGraph.withStatus(planExecution.getStatus()).withEndTs(planExecution.getEndTs());
      graphGenerationService.cacheOrchestrationGraph(orchestrationGraph);
    } catch (Exception e) {
      log.error("Cannot update Orchestration graph for ORCHESTRATION_END");
      throw e;
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
