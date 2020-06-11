package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.MongoStore;
import io.harness.engine.GraphGenerator;
import io.harness.engine.services.GraphGenerationService;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.presentation.Graph;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Redesign
@OwnedBy(CDC)
@Singleton
public class GraphGenerationServiceImpl implements GraphGenerationService {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private MongoStore mongoStore;
  @Inject private GraphGenerator graphGenerator;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public Graph generateGraph(String planExecutionId) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(planExecutionId);
    if (isEmpty(nodeExecutions)) {
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

  private String obtainStartingNodeExId(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream()
        .filter(node -> isEmpty(node.getParentId()) && isEmpty(node.getPreviousId()))
        .findFirst()
        .orElseThrow(() -> new InvalidRequestException("Starting node is not found"))
        .getUuid();
  }
}
