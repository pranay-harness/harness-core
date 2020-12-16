package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class OrchestrationStartEventHandler implements SyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    try {
      PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());

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
    } catch (Exception e) {
      log.error("[{}] event failed for plan [{}]", event.getEventType(), ambiance.getPlanExecutionId(), e);
      throw e;
    }
  }
}
