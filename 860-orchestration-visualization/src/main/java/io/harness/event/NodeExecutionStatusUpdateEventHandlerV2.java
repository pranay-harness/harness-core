package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.generator.OrchestrationAdjacencyListGenerator;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionStatusUpdateEventHandlerV2 implements AsyncOrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OutcomeService outcomeService;
  @Inject private OrchestrationAdjacencyListGenerator orchestrationAdjacencyListGenerator;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (nodeExecutionId == null) {
      return;
    }
    try {
      NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);

      OrchestrationGraph orchestrationGraph =
          graphGenerationService.getCachedOrchestrationGraph(ambiance.getPlanExecutionId());

      if (orchestrationGraph.getRootNodeIds().isEmpty()) {
        log.info("Setting rootNodeId: [{}] for plan [{}]", nodeExecutionId, ambiance.getPlanExecutionId());
        orchestrationGraph.getRootNodeIds().add(nodeExecutionId);
      }

      Map<String, GraphVertex> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();
      if (graphVertexMap.containsKey(nodeExecutionId)) {
        log.info("Updating graph vertex for [{}] with status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
            nodeExecution.getStatus(), ambiance.getPlanExecutionId());
        graphVertexMap.computeIfPresent(nodeExecutionId, (key, prevValue) -> {
          GraphVertex newValue = GraphVertexConverter.convertFrom(nodeExecution);
          if (StatusUtils.isFinalStatus(newValue.getStatus())) {
            newValue.setOutcomes(outcomeService.findAllByRuntimeId(ambiance.getPlanExecutionId(), nodeExecutionId));
          }
          return newValue;
        });
      } else {
        log.info("Adding graph vertex with id [{}] and status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
            nodeExecution.getStatus(), ambiance.getPlanExecutionId());
        orchestrationAdjacencyListGenerator.populateAdjacencyList(orchestrationGraph.getAdjacencyList(), nodeExecution);
      }
      graphGenerationService.cacheOrchestrationGraph(orchestrationGraph);
    } catch (Exception e) {
      log.error("[{}] event failed for [{}] for plan [{}]", event.getEventType(), nodeExecutionId,
          ambiance.getPlanExecutionId(), e);
    }
  }
}
