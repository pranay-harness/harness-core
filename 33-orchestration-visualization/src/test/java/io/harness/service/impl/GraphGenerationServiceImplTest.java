package io.harness.service.impl;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.beans.internal.EdgeListInternal;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.cache.SpringMongoStore;
import io.harness.category.element.UnitTests;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.executions.node.NodeExecutionRepository;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.state.StepType;
import io.harness.testlib.RealMongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Test class for {@link GraphGenerationServiceImpl}
 */
public class GraphGenerationServiceImplTest extends OrchestrationVisualizationTestBase {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionRepository nodeExecutionRepository;
  @Inject private SpringMongoStore mongoStore;
  @Mock @Named("EngineExecutorService") private ExecutorService executorService;
  @InjectMocks @Inject private GraphGenerationService graphGenerationService;

  @Before
  public void setUp() {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnOrchestrationGraph() {
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().build());
    NodeExecution dummyStart =
        NodeExecution.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().setupId("node1_plan").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("node1_plan")
                      .name("name")
                      .stepType(StepType.builder().type("DUMMY").build())
                      .identifier("identifier1")
                      .build())
            .build();
    nodeExecutionRepository.save(dummyStart);

    OrchestrationGraphDTO graphResponse = graphGenerationService.generateOrchestrationGraph(planExecution.getUuid());
    assertThat(graphResponse).isNotNull();
    assertThat(graphResponse.getAdjacencyList()).isNotNull();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap()).isNotEmpty();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(1);
    assertThat(graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)))
        .isNotNull();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)).getNextIds())
        .isEmpty();
    assertThat(graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)).getEdges())
        .isEmpty();
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnOrchestrationGraphWithCachedAdjList() {
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().build());
    NodeExecution dummyStart =
        NodeExecution.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().setupId("node1_plan").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("node1_plan")
                      .name("name")
                      .stepType(StepType.builder().type("DUMMY").build())
                      .identifier("identifier1")
                      .build())
            .build();
    nodeExecutionRepository.save(dummyStart);

    OrchestrationAdjacencyListInternal adjacencyListInternal =
        OrchestrationAdjacencyListInternal.builder()
            .graphVertexMap(ImmutableMap.of(dummyStart.getUuid(), GraphVertexConverter.convertFrom(dummyStart)))
            .adjacencyMap(ImmutableMap.of(dummyStart.getUuid(),
                EdgeListInternal.builder().edges(new ArrayList<>()).nextIds(new ArrayList<>()).build()))
            .build();
    OrchestrationGraph graphInternal = OrchestrationGraph.builder()
                                           .rootNodeIds(Lists.newArrayList(dummyStart.getUuid()))
                                           .cacheKey(planExecution.getUuid())
                                           .adjacencyList(adjacencyListInternal)
                                           .build();
    mongoStore.upsert(graphInternal, Duration.ofDays(10));

    OrchestrationGraphDTO graphResponse = graphGenerationService.generateOrchestrationGraph(planExecution.getUuid());
    assertThat(graphResponse).isNotNull();
    assertThat(graphResponse.getAdjacencyList()).isNotNull();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap()).isNotEmpty();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(1);
    assertThat(graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)))
        .isNotNull();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)).getNextIds())
        .isEmpty();
    assertThat(graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)).getEdges())
        .isEmpty();
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnOrchestrationGraphWithCachedAdjListWithNewAddedNodes() {
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().build());
    NodeExecution dummyStart =
        NodeExecution.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().setupId("node1_plan").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("node1_plan")
                      .name("name")
                      .stepType(StepType.builder().type("DUMMY").build())
                      .identifier("identifier1")
                      .build())
            .build();
    nodeExecutionRepository.save(dummyStart);

    NodeExecution dummyEnd =
        NodeExecution.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().setupId("node2_plan").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("node2_plan")
                      .name("nam2")
                      .stepType(StepType.builder().type("DUMMY").build())
                      .identifier("identifier2")
                      .build())
            .previousId(dummyStart.getUuid())
            .build();
    nodeExecutionRepository.save(dummyEnd);

    Map<String, GraphVertex> graphVertexMap = new HashMap<>();
    graphVertexMap.put(dummyStart.getUuid(), GraphVertexConverter.convertFrom(dummyStart));
    Map<String, EdgeListInternal> adjacencyMap = new HashMap<>();
    adjacencyMap.put(
        dummyStart.getUuid(), EdgeListInternal.builder().edges(new ArrayList<>()).nextIds(new ArrayList<>()).build());

    OrchestrationAdjacencyListInternal adjacencyListInternal =
        OrchestrationAdjacencyListInternal.builder().graphVertexMap(graphVertexMap).adjacencyMap(adjacencyMap).build();
    OrchestrationGraph graphInternal = OrchestrationGraph.builder()
                                           .rootNodeIds(Lists.newArrayList(dummyStart.getUuid()))
                                           .cacheKey(planExecution.getUuid())
                                           .adjacencyList(adjacencyListInternal)
                                           .build();
    mongoStore.upsert(graphInternal, Duration.ofDays(10));

    OrchestrationGraphDTO graphResponse = graphGenerationService.generateOrchestrationGraph(planExecution.getUuid());
    assertThat(graphResponse).isNotNull();

    OrchestrationAdjacencyList orchestrationAdjacencyList = graphResponse.getAdjacencyList();
    assertThat(orchestrationAdjacencyList).isNotNull();

    Map<String, GraphVertex> graphVertexMapResponse = orchestrationAdjacencyList.getGraphVertexMap();
    assertThat(graphVertexMapResponse).isNotEmpty();
    assertThat(graphVertexMapResponse.size()).isEqualTo(2);

    Map<String, EdgeList> adjacencyList = orchestrationAdjacencyList.getAdjacencyMap();
    assertThat(adjacencyList.get(graphResponse.getRootNodeIds().get(0))).isNotNull();
    assertThat(adjacencyList.get(graphResponse.getRootNodeIds().get(0)).getNextIds()).isNotEmpty();
    assertThat(adjacencyList.get(graphResponse.getRootNodeIds().get(0)).getNextIds())
        .containsExactlyInAnyOrder(dummyEnd.getUuid());
    assertThat(adjacencyList.get(graphResponse.getRootNodeIds().get(0)).getEdges()).isEmpty();

    assertThat(adjacencyList.get(dummyEnd.getUuid()).getNextIds()).isEmpty();
    assertThat(adjacencyList.get(dummyEnd.getUuid()).getEdges()).isEmpty();
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnPartialOrchestrationGraph() {
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().build());
    NodeExecution dummyStart =
        NodeExecution.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().setupId("node1_plan").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("node1_plan")
                      .name("dummyStart")
                      .stepType(StepType.builder().type("DUMMY").build())
                      .identifier("identifier1")
                      .build())
            .build();
    NodeExecution dummyFinish =
        NodeExecution.builder()
            .ambiance(Ambiance.builder()
                          .planExecutionId(planExecution.getUuid())
                          .levels(Collections.singletonList(Level.builder().setupId("node2_plan").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("node2_plan")
                      .name("dummyFinish")
                      .stepType(StepType.builder().type("DUMMY").build())
                      .identifier("identifier2")
                      .build())
            .build();
    nodeExecutionRepository.save(dummyStart);
    nodeExecutionRepository.save(dummyFinish);

    OrchestrationGraphDTO graphResponse = graphGenerationService.generatePartialOrchestrationGraph(
        dummyFinish.getNode().getUuid(), planExecution.getUuid());
    assertThat(graphResponse).isNotNull();
    assertThat(graphResponse.getRootNodeIds().get(0)).isEqualTo(dummyFinish.getUuid());
    assertThat(graphResponse.getAdjacencyList()).isNotNull();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap()).isNotEmpty();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(1);
    assertThat(graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)))
        .isNotNull();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)).getNextIds())
        .isEmpty();
    assertThat(graphResponse.getAdjacencyList().getAdjacencyMap().get(graphResponse.getRootNodeIds().get(0)).getEdges())
        .isEmpty();
  }

  private static Answer<?> executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }
}
