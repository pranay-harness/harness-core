package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.execution.status.Status.isFinalStatus;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyListInternal;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.service.GraphGenerationService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionStatusUpdateEventHandler implements OrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OutcomeService outcomeService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String nodeExecutionId = ambiance.obtainCurrentRuntimeId();
    if (nodeExecutionId == null) {
      return;
    }

    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);

    OrchestrationAdjacencyListInternal adjacencyListInternal =
        graphGenerationService.getCachedOrchestrationAdjacencyListInternal(ambiance.getPlanExecutionId());

    // return if there is no cache
    if (adjacencyListInternal == null) {
      return;
    }

    logger.info("Updating graph vertex for [{}]", nodeExecutionId);

    Map<String, GraphVertex> graphVertexMap = adjacencyListInternal.getGraphVertexMap();
    if (graphVertexMap.containsKey(nodeExecutionId)) {
      graphVertexMap.computeIfPresent(nodeExecutionId, (key, prevValue) -> {
        GraphVertex newValue = GraphVertexConverter.convertFrom(nodeExecution);
        if (isFinalStatus(newValue.getStatus())) {
          newValue.setOutcomes(outcomeService.findAllByRuntimeId(ambiance.getPlanExecutionId(), nodeExecutionId));
        }
        return newValue;
      });
      graphGenerationService.cacheOrchestrationAdjacencyListInternal(adjacencyListInternal);
    }
  }
}
