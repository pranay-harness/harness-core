package io.harness.event;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.DelegateInfoHelper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.data.structure.CollectionUtils;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.generator.OrchestrationAdjacencyListGenerator;
import io.harness.plan.IdentityPlanNode;
import io.harness.plan.NodeType;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class GraphStatusUpdateHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsOutcomeService pmsOutcomeService;
  @Inject private OrchestrationAdjacencyListGenerator orchestrationAdjacencyListGenerator;
  @Inject private DelegateInfoHelper delegateInfoHelper;

  public OrchestrationGraph handleEvent(String planExecutionId, String nodeExecutionId,
      OrchestrationEventType eventType, OrchestrationGraph orchestrationGraph) {
    if (isEmpty(nodeExecutionId)) {
      return orchestrationGraph;
    }
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    return handleEventV2(planExecutionId, nodeExecution, eventType, orchestrationGraph);
  }

  public OrchestrationGraph handleEventV2(String planExecutionId, NodeExecution nodeExecution,
      OrchestrationEventType eventType, OrchestrationGraph orchestrationGraph) {
    if (nodeExecution == null) {
      return orchestrationGraph;
    }
    String nodeExecutionId = nodeExecution.getUuid();
    try {
      if (orchestrationGraph.getRootNodeIds().isEmpty()) {
        log.info("[PMS_GRAPH]  Setting rootNodeId: [{}] for plan [{}]", nodeExecutionId, planExecutionId);
        orchestrationGraph.getRootNodeIds().add(nodeExecutionId);
      }

      Map<String, GraphVertex> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();
      if (graphVertexMap.containsKey(nodeExecutionId)) {
        if (nodeExecution.isOldRetry()) {
          log.info("[PMS_GRAPH]  Removing graph vertex with id [{}] and status [{}]. PlanExecutionId: [{}]",
              nodeExecutionId, nodeExecution.getStatus(), planExecutionId);
          orchestrationAdjacencyListGenerator.removeVertex(orchestrationGraph.getAdjacencyList(), nodeExecution);
        } else {
          updateGraphVertex(graphVertexMap, nodeExecution, planExecutionId);
        }
      } else if (!nodeExecution.isOldRetry()) {
        log.info("[PMS_GRAPH] Adding graph vertex with id [{}] and status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
            nodeExecution.getStatus(), planExecutionId);
        orchestrationAdjacencyListGenerator.addVertex(orchestrationGraph.getAdjacencyList(), nodeExecution);
      }
    } catch (Exception e) {
      log.error(
          "[PMS_GRAPH]  [{}] event failed for [{}] for plan [{}]", eventType, nodeExecutionId, planExecutionId, e);
      throw e;
    }
    return orchestrationGraph;
  }

  private void updateGraphVertex(
      Map<String, GraphVertex> graphVertexMap, NodeExecution nodeExecution, String planExecutionId) {
    String nodeExecutionId = nodeExecution.getUuid();
    log.info("[PMS_GRAPH] Updating graph vertex for [{}] with status [{}]. PlanExecutionId: [{}]", nodeExecutionId,
        nodeExecution.getStatus(), planExecutionId);
    graphVertexMap.computeIfPresent(nodeExecutionId, (key, prevValue) -> {
      GraphVertex newValue = convertFromNodeExecution(prevValue, nodeExecution);
      if (StatusUtils.isFinalStatus(newValue.getStatus())) {
        newValue.setOutcomeDocuments(PmsOutcomeMapper.convertJsonToOrchestrationMap(
            pmsOutcomeService.findAllOutcomesMapByRuntimeId(planExecutionId, nodeExecutionId)));
        newValue.setGraphDelegateSelectionLogParams(
            delegateInfoHelper.getDelegateInformationForGivenTask(nodeExecution.getExecutableResponses(),
                nodeExecution.getMode(), AmbianceUtils.getAccountId(nodeExecution.getAmbiance())));
      }
      return newValue;
    });
  }

  // Todo: Update only properties that will be changed. No need to construct full
  public GraphVertex convertFromNodeExecution(GraphVertex prevValue, NodeExecution nodeExecution) {
    String stepType = nodeExecution.getNode().getStepType().getType();
    if (nodeExecution.getNode().getNodeType().equals(NodeType.IDENTITY_PLAN_NODE)) {
      stepType = ((IdentityPlanNode) nodeExecution.getNode()).getOriginalStepType().getType();
    }
    return prevValue.toBuilder()
        .uuid(nodeExecution.getUuid())
        .ambiance(nodeExecution.getAmbiance())
        .planNodeId(nodeExecution.getNode().getUuid())
        .identifier(nodeExecution.getNode().getIdentifier())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(stepType)
        .status(nodeExecution.getStatus())
        .failureInfo(nodeExecution.getFailureInfo())
        .skipInfo(nodeExecution.getSkipInfo())
        .nodeRunInfo(nodeExecution.getNodeRunInfo())
        .stepParameters(nodeExecution.getPmsStepParameters())
        .mode(nodeExecution.getMode())
        .executableResponses(CollectionUtils.emptyIfNull(nodeExecution.getExecutableResponses()))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .skipType(nodeExecution.getNode().getSkipGraphType())
        .unitProgresses(nodeExecution.getUnitProgresses())
        .progressData(nodeExecution.getPmsProgressData())
        .build();
  }
}
