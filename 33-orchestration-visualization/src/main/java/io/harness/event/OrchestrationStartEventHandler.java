package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.service.GraphGenerationService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class OrchestrationStartEventHandler implements SyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    PlanExecution planExecution = planExecutionService.get(event.getAmbiance().getPlanExecutionId());

    log.info("Starting Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
        planExecution.getStatus());

    OrchestrationGraph graphInternal = OrchestrationGraph.builder()
                                           .cacheKey(planExecution.getUuid())
                                           .cacheParams(null)
                                           .cacheContextOrder(System.currentTimeMillis())
                                           .adjacencyList(OrchestrationAdjacencyListInternal.builder()
                                                              .graphVertexMap(new HashMap<>())
                                                              .adjacencyMap(new HashMap<>())
                                                              .build())
                                           .planExecutionId(planExecution.getUuid())
                                           .rootNodeIds(new ArrayList<>())
                                           .startTs(planExecution.getStartTs())
                                           .endTs(planExecution.getEndTs())
                                           .status(planExecution.getStatus())
                                           .build();

    graphGenerationService.cacheOrchestrationGraph(graphInternal);
  }
}
