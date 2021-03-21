package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EphemeralOrchestrationGraph;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.converter.EphemeralOrchestrationGraphConverter;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.cache.SpringMongoStore;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.dto.converter.OrchestrationGraphDTOConverter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.event.GraphStatusUpdateHelper;
import io.harness.event.NodeExecutionUpdateEventHandler;
import io.harness.event.OrchestrationEndEventHandler;
import io.harness.event.OrchestrationStartEventHandler;
import io.harness.event.PlanExecutionStatusUpdateEventHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.generator.OrchestrationAdjacencyListGenerator;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventLog;
import io.harness.repositories.orchestrationEventLog.OrchestrationEventLogRepository;
import io.harness.service.GraphGenerationService;
import io.harness.skip.service.VertexSkipperService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class GraphGenerationServiceImpl implements GraphGenerationService {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private SpringMongoStore mongoStore;
  @Inject private OrchestrationAdjacencyListGenerator orchestrationAdjacencyListGenerator;
  @Inject private VertexSkipperService vertexSkipperService;
  @Inject OrchestrationEventLogRepository orchestrationEventLogRepository;
  @Inject OrchestrationStartEventHandler orchestrationStartEventHandler;
  @Inject OrchestrationEndEventHandler orchestrationEndEventHandler;
  @Inject PlanExecutionStatusUpdateEventHandler planExecutionStatusUpdateEventHandler;
  @Inject NodeExecutionUpdateEventHandler nodeExecutionUpdateEventHandler;
  @Inject GraphStatusUpdateHelper graphStatusUpdateHelper;

  @Override
  public OrchestrationGraph getCachedOrchestrationGraph(String planExecutionId) {
    return mongoStore.get(OrchestrationGraph.ALGORITHM_ID, OrchestrationGraph.STRUCTURE_HASH, planExecutionId, null);
  }

  @Override
  public void cacheOrchestrationGraph(OrchestrationGraph orchestrationGraph) {
    mongoStore.upsert(orchestrationGraph, Duration.ofDays(10));
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
    return generatePartialGraph(
        obtainStartingIdFromSetupNodeId(orchestrationGraph.getAdjacencyList().getGraphVertexMap(), startingSetupNodeId),
        orchestrationGraph);
  }

  @Override
  public OrchestrationGraphDTO generatePartialOrchestrationGraphFromIdentifier(
      String identifier, String planExecutionId) {
    OrchestrationGraph orchestrationGraph = getCachedOrchestrationGraph(planExecutionId);
    if (orchestrationGraph == null) {
      orchestrationGraph = buildOrchestrationGraph(planExecutionId);
    }
    return generatePartialGraph(
        obtainStartingIdFromIdentifier(orchestrationGraph.getAdjacencyList().getGraphVertexMap(), identifier),
        orchestrationGraph);
  }

  public OrchestrationGraph buildOrchestrationGraphBasedOnLogs(String planExecutionId) {
    OrchestrationGraph orchestrationGraph = getCachedOrchestrationGraph(planExecutionId);
    List<OrchestrationEventLog> unprocessedEventLogs;
    log.info("Getting Unprocessed orchestrationEventLogs for planExecutionId [{}]", planExecutionId);
    if (orchestrationGraph == null) {
      unprocessedEventLogs = orchestrationEventLogRepository.findUnprocessedEvents(planExecutionId);
    } else {
      unprocessedEventLogs =
          orchestrationEventLogRepository.findUnprocessedEvents(planExecutionId, orchestrationGraph.getLastUpdatedAt());
    }
    log.info("Found [{}] unprocessed events", unprocessedEventLogs.size());
    long lastUpdatedAt = 0L;
    if (orchestrationGraph != null) {
      lastUpdatedAt = orchestrationGraph.getLastUpdatedAt();
    }
    if (!unprocessedEventLogs.isEmpty()) {
      for (OrchestrationEventLog orchestrationEventLog : unprocessedEventLogs) {
        log.info("Starting Processing Orchestration Event log with id [{}]", orchestrationEventLog.getId());

        OrchestrationEventType eventType = orchestrationEventLog.getEvent().getEventType();
        switch (eventType) {
          case NODE_EXECUTION_STATUS_UPDATE:
            orchestrationGraph =
                graphStatusUpdateHelper.handleEvent(orchestrationEventLog.getEvent(), orchestrationGraph);
            break;
          case ORCHESTRATION_START:
            orchestrationGraph = orchestrationStartEventHandler.handleEventFromLog(orchestrationEventLog.getEvent());
            break;
          case ORCHESTRATION_END:
            orchestrationGraph =
                orchestrationEndEventHandler.handleEvent(orchestrationEventLog.getEvent(), orchestrationGraph);
            break;
          case PLAN_EXECUTION_STATUS_UPDATE:
            orchestrationGraph =
                planExecutionStatusUpdateEventHandler.handleEvent(orchestrationEventLog.getEvent(), orchestrationGraph);
            break;
          case NODE_EXECUTION_UPDATE:
            orchestrationGraph =
                nodeExecutionUpdateEventHandler.handleEvent(orchestrationEventLog.getEvent(), orchestrationGraph);
            break;
          default:
            // do Nothing
        }
        lastUpdatedAt = orchestrationEventLog.getCreatedAt();
      }
    }
    if (unprocessedEventLogs.size() > 5) {
      log.warn("More than 5 Events Processed at a time for given planExecutionId:[{}]", planExecutionId);
    }
    if (orchestrationGraph != null) {
      orchestrationEventLogRepository.updateTtlForProcessedEvents(unprocessedEventLogs);
      orchestrationGraph.setLastUpdatedAt(lastUpdatedAt);
      cacheOrchestrationGraph(orchestrationGraph);
    }
    log.info("Processing of [{}] orchestration event logs completed", unprocessedEventLogs.size());
    return orchestrationGraph;
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
    return graphVertexMap.values()
        .stream()
        .filter(vertex -> vertex.getPlanNodeId().equals(startingSetupNodeId))
        .collect(Collectors.collectingAndThen(Collectors.toList(),
            list -> {
              if (list.isEmpty()) {
                throw new InvalidRequestException("No nodes found for setupNodeId [" + startingSetupNodeId + "]");
              }
              if (list.size() > 1) {
                throw new InvalidRequestException("Repeated setupNodeIds are not supported. Check the plan for ["
                    + startingSetupNodeId + "] planNodeId");
              }

              return list.get(0);
            }))
        .getUuid();
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
