package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Graph;
import io.harness.beans.OrchestrationGraphInternal;
import io.harness.cache.SpringMongoStore;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraph;
import io.harness.dto.converter.OrchestrationGraphConverter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.generator.GraphGenerator;
import io.harness.service.GraphGenerationService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Redesign
@OwnedBy(HarnessTeam.CDC)
@Singleton
public class GraphGenerationServiceImpl implements GraphGenerationService {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private SpringMongoStore mongoStore;
  @Inject private GraphGenerator graphGenerator;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public Graph generateGraph(String planExecutionId) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(planExecutionId);
    if (EmptyPredicate.isEmpty(nodeExecutions)) {
      throw new InvalidRequestException("No nodes found for planExecutionId [" + planExecutionId + "]");
    }

    long lastUpdated = nodeExecutions.stream().map(NodeExecution::getLastUpdatedAt).max(Long::compare).orElse(0L);

    Graph cachedGraph = mongoStore.get(Graph.ALGORITHM_ID, Graph.STRUCTURE_HASH, planExecutionId, null);
    if (cachedGraph != null && cachedGraph.getCacheContextOrder() >= lastUpdated) {
      return cachedGraph;
    }

    Graph graphToBeCached = Graph.builder()
                                .cacheKey(planExecutionId)
                                .cacheContextOrder(lastUpdated)
                                .cacheParams(null)
                                .planExecutionId(planExecution.getUuid())
                                .startTs(planExecution.getStartTs())
                                .endTs(planExecution.getEndTs())
                                .status(planExecution.getStatus())
                                .graphVertex(graphGenerator.generateGraphVertexStartingFrom(
                                    obtainStartingNodeExId(nodeExecutions), nodeExecutions))
                                .build();

    executorService.submit(() -> mongoStore.upsert(graphToBeCached, Duration.ofDays(10)));
    return graphToBeCached;
  }

  @Override
  public OrchestrationGraph generateOrchestrationGraph(String planExecutionId) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(planExecutionId);
    if (isEmpty(nodeExecutions)) {
      throw new InvalidRequestException("No nodes found for planExecutionId [" + planExecutionId + "]");
    }

    long lastUpdated = nodeExecutions.stream().map(NodeExecution::getLastUpdatedAt).max(Long::compare).orElse(0L);

    OrchestrationGraphInternal cachedGraph = mongoStore.get(
        OrchestrationGraphInternal.ALGORITHM_ID, OrchestrationGraphInternal.STRUCTURE_HASH, planExecutionId, null);
    if (cachedGraph != null && cachedGraph.getCacheContextOrder() >= lastUpdated) {
      return OrchestrationGraphConverter.convertFrom(cachedGraph);
    }

    String rootNodeId = obtainStartingNodeExId(nodeExecutions);

    OrchestrationGraphInternal orchestrationGraphInternal =
        OrchestrationGraphInternal.builder()
            .cacheKey(planExecutionId)
            .cacheContextOrder(lastUpdated)
            .cacheParams(null)
            .planExecutionId(planExecution.getUuid())
            .startTs(planExecution.getStartTs())
            .endTs(planExecution.getEndTs())
            .status(planExecution.getStatus())
            .rootNodeId(rootNodeId)
            .adjacencyList(graphGenerator.generateAdjacencyList(rootNodeId, nodeExecutions))
            .build();

    executorService.submit(() -> mongoStore.upsert(orchestrationGraphInternal, Duration.ofDays(10)));
    return OrchestrationGraphConverter.convertFrom(orchestrationGraphInternal);
  }

  private String obtainStartingNodeExId(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream()
        .filter(node -> EmptyPredicate.isEmpty(node.getParentId()) && EmptyPredicate.isEmpty(node.getPreviousId()))
        .findFirst()
        .orElseThrow(() -> new InvalidRequestException("Starting node is not found"))
        .getUuid();
  }
}
