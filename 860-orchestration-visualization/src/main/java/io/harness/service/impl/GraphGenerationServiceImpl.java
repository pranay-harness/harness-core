package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EphemeralOrchestrationGraph;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationEventLog;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.converter.EphemeralOrchestrationGraphConverter;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.cache.SpringCacheEntity;
import io.harness.cache.SpringMongoStore;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.dto.converter.OrchestrationGraphDTOConverter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.event.GraphStatusUpdateHelper;
import io.harness.event.PlanExecutionStatusUpdateEventHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.generator.OrchestrationAdjacencyListGenerator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.repositories.orchestrationEventLog.OrchestrationEventLogRepository;
import io.harness.service.GraphGenerationService;
import io.harness.skip.service.VertexSkipperService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class GraphGenerationServiceImpl implements GraphGenerationService {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private SpringMongoStore mongoStore;
  @Inject private OrchestrationAdjacencyListGenerator orchestrationAdjacencyListGenerator;
  @Inject private VertexSkipperService vertexSkipperService;
  @Inject private OrchestrationEventLogRepository orchestrationEventLogRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private GraphStatusUpdateHelper graphStatusUpdateHelper;
  @Inject private PlanExecutionStatusUpdateEventHandler planExecutionStatusUpdateEventHandler;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  @Override
  public void updateGraph(String planExecutionId) {
    long startTs = System.currentTimeMillis();
    OrchestrationGraph orchestrationGraph = getCachedOrchestrationGraph(planExecutionId);
    if (orchestrationGraph == null) {
      log.warn("Orchestration Graph not yet generated. Passing on to next iteration");
      return;
    }

    long lastUpdatedAt = orchestrationGraph.getLastUpdatedAt();
    List<OrchestrationEventLog> unprocessedEventLogs =
        orchestrationEventLogRepository.findUnprocessedEvents(planExecutionId, lastUpdatedAt);
    if (!unprocessedEventLogs.isEmpty()) {
      log.info("Found [{}] unprocessed events", unprocessedEventLogs.size());
      for (OrchestrationEventLog orchestrationEventLog : unprocessedEventLogs) {
        // Todo: Remove the event in next release
        OrchestrationEventType orchestrationEventType = orchestrationEventLog.getEvent() != null
            ? orchestrationEventLog.getEvent().getEventType()
            : orchestrationEventLog.getOrchestrationEventType();
        if (orchestrationEventType == OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE) {
          orchestrationGraph = planExecutionStatusUpdateEventHandler.handleEvent(planExecutionId, orchestrationGraph);
        } else {
          String nodeExecutionId = orchestrationEventLog.getNodeExecutionId();
          orchestrationGraph = graphStatusUpdateHelper.handleEvent(
              planExecutionId, nodeExecutionId, orchestrationEventType, orchestrationGraph);
        }
        lastUpdatedAt = orchestrationEventLog.getCreatedAt();
      }
    }
    orchestrationEventLogRepository.updateTtlForProcessedEvents(unprocessedEventLogs);
    orchestrationGraph.setLastUpdatedAt(lastUpdatedAt);
    cacheOrchestrationGraph(orchestrationGraph);
    log.info("Processing of [{}] orchestration event logs completed in [{}ms]", unprocessedEventLogs.size(),
        System.currentTimeMillis() - startTs);
  }

  @Override
  public OrchestrationGraph getCachedOrchestrationGraph(String planExecutionId) {
    return mongoStore.get(OrchestrationGraph.ALGORITHM_ID, OrchestrationGraph.STRUCTURE_HASH, planExecutionId, null);
  }

  @Override
  public void cacheOrchestrationGraph(OrchestrationGraph orchestrationGraph) {
    mongoStore.upsert(orchestrationGraph, SpringCacheEntity.TTL);
  }

  @Deprecated
  @Override
  public OrchestrationGraphDTO generateOrchestrationGraph(String planExecutionId) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(planExecutionId);
    if (isEmpty(nodeExecutions)) {
      throw new InvalidRequestException("No nodes found for planExecutionId [" + planExecutionId + "]");
    }

    String rootNodeId = obtainStartingNodeExId(nodeExecutions);

    OrchestrationGraph orchestrationGraph = getCachedOrchestrationGraph(planExecutionId);
    if (orchestrationGraph != null) {
      OrchestrationAdjacencyListInternal adjacencyList = orchestrationGraph.getAdjacencyList();
      List<NodeExecution> newNodeExecutions =
          nodeExecutions.stream()
              .filter(node -> !adjacencyList.getGraphVertexMap().containsKey(node.getUuid()))
              .collect(Collectors.toList());
      if (!newNodeExecutions.isEmpty()) {
        orchestrationAdjacencyListGenerator.populateAdjacencyList(adjacencyList, newNodeExecutions);
        cacheOrchestrationGraph(orchestrationGraph);
      }
      EphemeralOrchestrationGraph ephemeralOrchestrationGraph =
          EphemeralOrchestrationGraphConverter.convertFrom(orchestrationGraph);

      vertexSkipperService.removeSkippedVertices(ephemeralOrchestrationGraph);
      return OrchestrationGraphDTOConverter.convertFrom(ephemeralOrchestrationGraph);
    }

    OrchestrationGraph graph =
        OrchestrationGraph.builder()
            .cacheKey(planExecutionId)
            .cacheContextOrder(System.currentTimeMillis())
            .cacheParams(null)
            .planExecutionId(planExecution.getUuid())
            .startTs(planExecution.getStartTs())
            .endTs(planExecution.getEndTs())
            .status(planExecution.getStatus())
            .rootNodeIds(Lists.newArrayList(rootNodeId))
            .adjacencyList(orchestrationAdjacencyListGenerator.generateAdjacencyList(rootNodeId, nodeExecutions, false))
            .build();

    cacheOrchestrationGraph(graph);

    EphemeralOrchestrationGraph ephemeralOrchestrationGraph = EphemeralOrchestrationGraphConverter.convertFrom(graph);

    vertexSkipperService.removeSkippedVertices(ephemeralOrchestrationGraph);
    return OrchestrationGraphDTOConverter.convertFrom(ephemeralOrchestrationGraph);
  }

  @Override
  public OrchestrationGraphDTO generateOrchestrationGraphV2(String planExecutionId) {
    OrchestrationGraph cachedOrchestrationGraph = getCachedOrchestrationGraph(planExecutionId);
    if (cachedOrchestrationGraph == null) {
      cachedOrchestrationGraph = buildOrchestrationGraph(planExecutionId);
    }
    EphemeralOrchestrationGraph ephemeralOrchestrationGraph =
        EphemeralOrchestrationGraphConverter.convertFrom(cachedOrchestrationGraph);
    vertexSkipperService.removeSkippedVertices(ephemeralOrchestrationGraph);
    return OrchestrationGraphDTOConverter.convertFrom(ephemeralOrchestrationGraph);
  }

  @Override
  public OrchestrationGraphDTO generatePartialOrchestrationGraphFromSetupNodeId(
      String startingSetupNodeId, String planExecutionId) {
    OrchestrationGraph orchestrationGraph = getCachedOrchestrationGraph(planExecutionId);
    if (orchestrationGraph == null) {
      orchestrationGraph = buildOrchestrationGraph(planExecutionId);
    }

    String startingNodeId =
        obtainStartingIdFromSetupNodeId(orchestrationGraph.getAdjacencyList().getGraphVertexMap(), startingSetupNodeId);
    return generatePartialGraph(startingNodeId, orchestrationGraph);
  }

  @Override
  public OrchestrationGraphDTO generatePartialOrchestrationGraphFromIdentifier(
      String identifier, String planExecutionId) {
    OrchestrationGraph orchestrationGraph = getCachedOrchestrationGraph(planExecutionId);
    if (orchestrationGraph == null) {
      orchestrationGraph = buildOrchestrationGraph(planExecutionId);
    }
    String startingId =
        obtainStartingIdFromIdentifier(orchestrationGraph.getAdjacencyList().getGraphVertexMap(), identifier);
    if (startingId == null) {
      return null;
    }
    return generatePartialGraph(startingId, orchestrationGraph);
  }

  public OrchestrationGraph buildOrchestrationGraph(String planExecutionId) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(planExecutionId);
    if (isEmpty(nodeExecutions)) {
      throw new InvalidRequestException("No nodes found for planExecutionId [" + planExecutionId + "]");
    }

    String rootNodeId = obtainStartingNodeExId(nodeExecutions);

    OrchestrationGraph graph =
        OrchestrationGraph.builder()
            .cacheKey(planExecutionId)
            .cacheContextOrder(System.currentTimeMillis())
            .cacheParams(null)
            .planExecutionId(planExecution.getUuid())
            .startTs(planExecution.getStartTs())
            .endTs(planExecution.getEndTs())
            .status(planExecution.getStatus())
            .rootNodeIds(Lists.newArrayList(rootNodeId))
            .adjacencyList(orchestrationAdjacencyListGenerator.generateAdjacencyList(rootNodeId, nodeExecutions, true))
            .build();

    cacheOrchestrationGraph(graph);
    return graph;
  }

  private OrchestrationGraphDTO generatePartialGraph(String startId, OrchestrationGraph orchestrationGraph) {
    EphemeralOrchestrationGraph ephemeralOrchestrationGraph =
        EphemeralOrchestrationGraph.builder()
            .planExecutionId(orchestrationGraph.getPlanExecutionId())
            .rootNodeIds(Lists.newArrayList(startId))
            .startTs(orchestrationGraph.getStartTs())
            .endTs(orchestrationGraph.getEndTs())
            .status(orchestrationGraph.getStatus())
            .adjacencyList(orchestrationAdjacencyListGenerator.generatePartialAdjacencyList(
                startId, orchestrationGraph.getAdjacencyList()))
            .build();

    vertexSkipperService.removeSkippedVertices(ephemeralOrchestrationGraph);

    return OrchestrationGraphDTOConverter.convertFrom(ephemeralOrchestrationGraph);
  }

  private String obtainStartingIdFromSetupNodeId(Map<String, GraphVertex> graphVertexMap, String startingSetupNodeId) {
    List<GraphVertex> vertexList = graphVertexMap.values()
                                       .stream()
                                       .filter(vertex -> vertex.getPlanNodeId().equals(startingSetupNodeId))
                                       .collect(Collectors.toList());

    if (vertexList.size() == 0) {
      return null;
    } else if (vertexList.size() == 1) {
      return vertexList.get(0).getUuid();
    } else {
      throw new InvalidRequestException(
          "Repeated setupNodeIds are not supported. Check the plan for [" + startingSetupNodeId + "] planNodeId");
    }
  }

  private String obtainStartingIdFromIdentifier(Map<String, GraphVertex> graphVertexMap, String identifier) {
    return graphVertexMap.values()
        .stream()
        .filter(vertex -> vertex.getIdentifier().equals(identifier))
        .collect(Collectors.collectingAndThen(Collectors.toList(),
            list -> {
              if (list.isEmpty()) {
                throw new InvalidRequestException("No nodes found for identifier [" + identifier + "]");
              }
              if (list.size() > 1) {
                throw new InvalidRequestException(
                    "Repeated identifiers are not supported. Check the plan for [" + identifier + "] identifier");
              }

              return list.get(0);
            }))
        .getUuid();
  }

  private String obtainStartingNodeExId(List<NodeExecution> nodeExecutions) {
    return nodeExecutions.stream()
        .filter(node -> EmptyPredicate.isEmpty(node.getParentId()) && EmptyPredicate.isEmpty(node.getPreviousId()))
        .findFirst()
        .orElseThrow(() -> new InvalidRequestException("Starting node is not found"))
        .getUuid();
  }
}
