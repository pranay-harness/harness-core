package io.harness.event;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;
import static io.harness.execution.status.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.OrchestrationVisualizationTest;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.OrchestrationGraphInternal;
import io.harness.cache.SpringMongoStore;
import io.harness.category.element.UnitTests;
import io.harness.data.OutcomeInstance;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.outcomes.OutcomeRepository;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.state.StepType;
import io.harness.testlib.RealMongo;
import io.harness.utils.DummyOutcome;
import org.assertj.core.util.Maps;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Spy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Test class for {@link NodeExecutionStatusUpdateEventHandlerV2}
 */
public class NodeExecutionStatusUpdateEventHandlerV2Test extends OrchestrationVisualizationTest {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private SpringMongoStore mongoStore;

  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Spy private GraphGenerationService graphGenerationService;
  @Inject private OutcomeRepository outcomeRepository;
  @Inject private NodeExecutionStatusUpdateEventHandlerV2 eventHandlerV2;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldDoNothingIfRuntimeIdIsNull() {
    String planExecutionId = generateUuid();
    OrchestrationEvent event =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecutionId)
                          .levels(Collections.singletonList(Level.builder().runtimeId(null).build()))
                          .build())
            .eventType(NODE_EXECUTION_STATUS_UPDATE)
            .build();
    eventHandlerV2.handleEvent(event);

    verify(graphGenerationService, never()).getCachedOrchestrationGraphInternal(planExecutionId);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldAddRootNodeIdToTheGraphAndAddVertex() {
    // creating PlanExecution
    PlanExecution planExecution =
        PlanExecution.builder().uuid(generateUuid()).startTs(System.currentTimeMillis()).status(Status.RUNNING).build();
    planExecutionService.save(planExecution);

    // creating NodeExecution
    NodeExecution dummyStart = NodeExecution.builder()
                                   .uuid(generateUuid())
                                   .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid(generateUuid())
                                             .name("name")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("identifier1")
                                             .build())
                                   .build();
    nodeExecutionService.save(dummyStart);

    // creating cached graph
    OrchestrationGraphInternal cachedGraph = OrchestrationGraphInternal.builder()
                                                 .cacheKey(planExecution.getUuid())
                                                 .cacheParams(null)
                                                 .cacheContextOrder(System.currentTimeMillis())
                                                 .adjacencyList(OrchestrationAdjacencyList.builder()
                                                                    .graphVertexMap(new HashMap<>())
                                                                    .adjacencyList(new HashMap<>())
                                                                    .build())
                                                 .planExecutionId(planExecution.getUuid())
                                                 .rootNodeIds(new ArrayList<>())
                                                 .startTs(planExecution.getStartTs())
                                                 .endTs(planExecution.getEndTs())
                                                 .status(planExecution.getStatus())
                                                 .build();
    mongoStore.upsert(cachedGraph, Duration.ofDays(10));

    // creating event
    OrchestrationEvent event =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().runtimeId(dummyStart.getUuid()).build()))
                          .build())
            .eventType(NODE_EXECUTION_STATUS_UPDATE)
            .build();
    eventHandlerV2.handleEvent(event);

    Awaitility.await().atMost(2, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
      OrchestrationGraphInternal graphInternal =
          graphGenerationService.getCachedOrchestrationGraphInternal(planExecution.getUuid());
      return !graphInternal.getRootNodeIds().isEmpty();
    });

    OrchestrationGraphInternal updatedGraph =
        graphGenerationService.getCachedOrchestrationGraphInternal(planExecution.getUuid());

    assertThat(updatedGraph).isNotNull();
    assertThat(updatedGraph.getPlanExecutionId()).isEqualTo(planExecution.getUuid());
    assertThat(updatedGraph.getStartTs()).isEqualTo(planExecution.getStartTs());
    assertThat(updatedGraph.getEndTs()).isNull();
    assertThat(updatedGraph.getRootNodeIds()).containsExactlyInAnyOrder(dummyStart.getUuid());
    assertThat(updatedGraph.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(1);
    assertThat(updatedGraph.getAdjacencyList().getAdjacencyList().size()).isEqualTo(1);
    assertThat(updatedGraph.getStatus()).isEqualTo(planExecution.getStatus());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldUpdateExistingVertexInGraphAndAddOutcomes() {
    // creating PlanExecution
    PlanExecution planExecution =
        PlanExecution.builder().uuid(generateUuid()).startTs(System.currentTimeMillis()).status(Status.RUNNING).build();
    planExecutionService.save(planExecution);

    // creating NodeExecution
    NodeExecution dummyStart = NodeExecution.builder()
                                   .uuid(generateUuid())
                                   .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                                   .mode(ExecutionMode.SYNC)
                                   .status(SUCCEEDED)
                                   .node(PlanNode.builder()
                                             .uuid(generateUuid())
                                             .name("name")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("identifier1")
                                             .build())
                                   .build();
    nodeExecutionService.save(dummyStart);

    // creating cached graph
    OrchestrationGraphInternal cachedGraph =
        OrchestrationGraphInternal.builder()
            .cacheKey(planExecution.getUuid())
            .cacheParams(null)
            .cacheContextOrder(System.currentTimeMillis())
            .adjacencyList(OrchestrationAdjacencyList.builder()
                               .graphVertexMap(Maps.newHashMap(
                                   dummyStart.getUuid(), convertNodeExecutionWithStatusSucceeded(dummyStart)))
                               .adjacencyList(Maps.newHashMap(dummyStart.getUuid(),
                                   EdgeList.builder().edges(new ArrayList<>()).nextIds(new ArrayList<>()).build()))
                               .build())
            .planExecutionId(planExecution.getUuid())
            .rootNodeIds(Lists.newArrayList(dummyStart.getUuid()))
            .startTs(planExecution.getStartTs())
            .endTs(planExecution.getEndTs())
            .status(planExecution.getStatus())
            .build();
    mongoStore.upsert(cachedGraph, Duration.ofDays(10));

    // creating outcome
    DummyOutcome dummyOutcome = new DummyOutcome("outcome");
    OutcomeInstance outcome = OutcomeInstance.builder()
                                  .planExecutionId(planExecution.getUuid())
                                  .producedBy(Level.fromPlanNode(dummyStart.getUuid(), dummyStart.getNode()))
                                  .createdAt(System.currentTimeMillis())
                                  .outcome(dummyOutcome)
                                  .build();
    outcomeRepository.save(outcome);

    // creating event
    OrchestrationEvent event =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().runtimeId(dummyStart.getUuid()).build()))
                          .build())
            .eventType(NODE_EXECUTION_STATUS_UPDATE)
            .build();
    eventHandlerV2.handleEvent(event);

    Awaitility.await().atMost(2, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
      OrchestrationGraphInternal graphInternal =
          graphGenerationService.getCachedOrchestrationGraphInternal(planExecution.getUuid());
      return graphInternal.getAdjacencyList().getGraphVertexMap().get(dummyStart.getUuid()).getStatus() == SUCCEEDED;
    });

    OrchestrationGraphInternal updatedGraph =
        graphGenerationService.getCachedOrchestrationGraphInternal(planExecution.getUuid());

    assertThat(updatedGraph).isNotNull();
    assertThat(updatedGraph.getPlanExecutionId()).isEqualTo(planExecution.getUuid());
    assertThat(updatedGraph.getStartTs()).isEqualTo(planExecution.getStartTs());
    assertThat(updatedGraph.getEndTs()).isNull();
    assertThat(updatedGraph.getRootNodeIds()).containsExactlyInAnyOrder(dummyStart.getUuid());

    Map<String, GraphVertex> graphVertexMap = updatedGraph.getAdjacencyList().getGraphVertexMap();
    assertThat(graphVertexMap.size()).isEqualTo(1);
    assertThat(graphVertexMap.get(dummyStart.getUuid()).getStatus()).isEqualTo(SUCCEEDED);
    assertThat(graphVertexMap.get(dummyStart.getUuid()).getOutcomes()).containsExactlyInAnyOrder(dummyOutcome);
    assertThat(updatedGraph.getAdjacencyList().getAdjacencyList().size()).isEqualTo(1);
    assertThat(updatedGraph.getStatus()).isEqualTo(planExecution.getStatus());
  }

  private GraphVertex convertNodeExecutionWithStatusSucceeded(NodeExecution nodeExecution) {
    return GraphVertex.builder()
        .uuid(nodeExecution.getUuid())
        .planNodeId(nodeExecution.getNode().getUuid())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(SUCCEEDED)
        .failureInfo(nodeExecution.getFailureInfo())
        .stepParameters(nodeExecution.getResolvedStepParameters())
        .mode(nodeExecution.getMode())
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .build();
  }
}
