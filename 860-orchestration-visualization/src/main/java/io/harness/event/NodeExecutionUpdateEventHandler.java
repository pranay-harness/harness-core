package io.harness.event;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.generator.OrchestrationAdjacencyListGenerator;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class NodeExecutionUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OrchestrationAdjacencyListGenerator orchestrationAdjacencyListGenerator;

  public OrchestrationGraph handleEvent(OrchestrationEvent event, OrchestrationGraph orchestrationGraph) {
    NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
    String nodeExecutionId = nodeExecutionProto.getUuid();
    String planExecutionId = nodeExecutionProto.getAmbiance().getPlanExecutionId();
    if (isEmpty(nodeExecutionId)) {
      return orchestrationGraph;
    }
    try {
      NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
      Map<String, GraphVertex> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();
      if (graphVertexMap.containsKey(nodeExecutionId)) {
        GraphVertex graphVertex = graphVertexMap.get(nodeExecutionId);
        if (nodeExecution.isOldRetry()) {
          log.info("Removing graph vertex with id [{}] and status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
              nodeExecution.getStatus(), planExecutionId);
          orchestrationAdjacencyListGenerator.removeVertex(orchestrationGraph.getAdjacencyList(), nodeExecution);
        }
        graphVertex.setProgressDataMap(nodeExecution.getProgressDataMap());
        graphVertex.setUnitProgresses(nodeExecution.getUnitProgresses());
        graphVertexMap.put(nodeExecutionId, graphVertex);
        return orchestrationGraph;
      }
    } catch (Exception e) {
      log.error("[{}] event failed for [{}] for plan [{}]", event.getEventType(), nodeExecutionId, planExecutionId, e);
      throw e;
    }
    return orchestrationGraph;
  }

  @Override
  public void handleEvent(OrchestrationEvent event) {}
}
