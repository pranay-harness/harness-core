package io.harness.event;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.DelegateInfoHelper;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.generator.OrchestrationAdjacencyListGenerator;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class GraphStatusUpdateHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private PmsOutcomeService pmsOutcomeService;
  @Inject private OrchestrationAdjacencyListGenerator orchestrationAdjacencyListGenerator;
  @Inject private DelegateInfoHelper delegateInfoHelper;

  public OrchestrationGraph handleEvent(OrchestrationEvent event, OrchestrationGraph orchestrationGraph) {
    // ToDo(Alexei) rewrite when proto will contain all the fields
    NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
    String nodeExecutionId = nodeExecutionProto.getUuid();
    String planExecutionId = nodeExecutionProto.getAmbiance().getPlanExecutionId();
    if (isEmpty(nodeExecutionId)) {
      return orchestrationGraph;
    }
    try {
      log.info("[{}] event log handler started for [{}] for plan [{}]", event.getEventType(), nodeExecutionId,
          planExecutionId);

      NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);

      if (orchestrationGraph.getRootNodeIds().isEmpty()) {
        log.info("Setting rootNodeId: [{}] for plan [{}]", nodeExecutionId, planExecutionId);
        orchestrationGraph.getRootNodeIds().add(nodeExecutionId);
      }

      Map<String, GraphVertex> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();
      if (graphVertexMap.containsKey(nodeExecutionId)) {
        if (nodeExecution.isOldRetry()) {
          log.info("Removing graph vertex with id [{}] and status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
              nodeExecution.getStatus(), planExecutionId);
          orchestrationAdjacencyListGenerator.removeVertex(orchestrationGraph.getAdjacencyList(), nodeExecution);
        } else {
          updateGraphVertex(graphVertexMap, nodeExecution, planExecutionId);
        }
      } else {
        log.info("Adding graph vertex with id [{}] and status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
            nodeExecution.getStatus(), planExecutionId);
        orchestrationAdjacencyListGenerator.addVertex(orchestrationGraph.getAdjacencyList(), nodeExecution);
      }
      log.info("[{}] event log handler completed for [{}] for plan [{}]", event.getEventType(), nodeExecutionId,
          planExecutionId);
    } catch (Exception e) {
      log.error("[{}] event failed for [{}] for plan [{}]", event.getEventType(), nodeExecutionId, planExecutionId, e);
      throw e;
    }
    return orchestrationGraph;
  }

  private void updateGraphVertex(
      Map<String, GraphVertex> graphVertexMap, NodeExecution nodeExecution, String planExecutionId) {
    String nodeExecutionId = nodeExecution.getUuid();
    log.info("Updating graph vertex for [{}] with status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
        nodeExecution.getStatus(), planExecutionId);
    graphVertexMap.computeIfPresent(nodeExecutionId, (key, prevValue) -> {
      GraphVertex newValue = GraphVertexConverter.convertFrom(nodeExecution);
      if (StatusUtils.isFinalStatus(newValue.getStatus())) {
        newValue.setOutcomeDocuments(PmsOutcomeMapper.convertJsonToDocument(
            pmsOutcomeService.findAllByRuntimeId(planExecutionId, nodeExecutionId, true)));
        newValue.setDelegateSelectionLogParams(
            delegateInfoHelper.getDelegateInformationForGivenTask(nodeExecution.getExecutableResponses(),
                nodeExecution.getMode(), AmbianceUtils.getAccountId(nodeExecution.getAmbiance())));
      }
      return newValue;
    });
  }
}
