package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionStatusUpdateEventHandler implements OrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OutcomeService outcomeService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (nodeExecutionId == null) {
      return;
    }

    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);

    OrchestrationGraph graph = graphGenerationService.getCachedOrchestrationGraph(ambiance.getPlanExecutionId());

    // return if there is no cache
    if (graph == null) {
      log.info("Orchestration graph cache is null");
      return;
    }

    Map<String, GraphVertex> graphVertexMap = graph.getAdjacencyList().getGraphVertexMap();
    if (graphVertexMap.containsKey(nodeExecutionId)) {
      log.info("Updating graph vertex for [{}] with status [{}]", nodeExecutionId, nodeExecution.getStatus());
      graphVertexMap.computeIfPresent(nodeExecutionId, (key, prevValue) -> {
        GraphVertex newValue = GraphVertexConverter.convertFrom(nodeExecution);
        if (StatusUtils.isFinalStatus(newValue.getStatus())) {
          newValue.setOutcomes(outcomeService.findAllByRuntimeId(ambiance.getPlanExecutionId(), nodeExecutionId));
        }
        return newValue;
      });
      graphGenerationService.cacheOrchestrationGraph(graph);
    }
  }
}
