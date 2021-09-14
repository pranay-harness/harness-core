/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class OrchestrationStartEventHandler implements OrchestrationStartObserver {
  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  @Override
  public void onStart(OrchestrationStartInfo orchestrationStartInfo) {
    Ambiance ambiance = orchestrationStartInfo.getAmbiance();
    OrchestrationGraph orchestrationGraph = handleEventFromLog(ambiance);
    if (orchestrationGraph != null) {
      graphGenerationService.cacheOrchestrationGraph(orchestrationGraph);
    }
  }

  public OrchestrationGraph handleEventFromLog(Ambiance ambiance) {
    try {
      PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());

      log.info("Starting Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());

      return OrchestrationGraph.builder()
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

    } catch (Exception e) {
      log.error("Failed to handle event from log for [{}] planExecutionId", ambiance.getPlanExecutionId(), e);
      throw e;
    }
  }
}
