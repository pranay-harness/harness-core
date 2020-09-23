package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.OrchestrationGraphInternal;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.service.GraphGenerationService;
import lombok.extern.slf4j.Slf4j;

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

    logger.info("Starting Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
        planExecution.getStatus());

    OrchestrationGraphInternal graphInternal = OrchestrationGraphInternal.builder()
                                                   .cacheKey(planExecution.getUuid())
                                                   .cacheParams(null)
                                                   .cacheContextOrder(System.currentTimeMillis())
                                                   .adjacencyList(OrchestrationAdjacencyList.builder()
                                                                      .graphVertexMap(new HashMap<>())
                                                                      .adjacencyList(new HashMap<>())
                                                                      .build())
                                                   .planExecutionId(planExecution.getUuid())
                                                   .rootNodeId(null)
                                                   .startTs(planExecution.getStartTs())
                                                   .endTs(planExecution.getEndTs())
                                                   .status(planExecution.getStatus())
                                                   .build();

    graphGenerationService.cacheOrchestrationGraphInternal(graphInternal);
  }
}
