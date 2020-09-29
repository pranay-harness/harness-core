package io.harness.service.impl;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationVisualizationTest;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.beans.EdgeList;
import io.harness.beans.Graph;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.OrchestrationGraphInternal;
import io.harness.beans.converter.GraphVertexConverter;
import io.harness.cache.SpringMongoStore;
import io.harness.category.element.UnitTests;
import io.harness.dto.OrchestrationGraph;
import io.harness.engine.executions.node.NodeExecutionRepository;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.testlib.RealMongo;
import io.harness.utils.DummyForkStepParameters;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Test class for {@link GraphGenerationServiceImpl}
 */
public class GraphGenerationServiceImplTest extends OrchestrationVisualizationTest {
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
  public void shouldThrowInvalidRequestExceptionWhenPlanExecutionIdIsNull() {
    assertThatThrownBy(() -> graphGenerationService.generateGraph(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("The given id must not be null!");
  }

  @Test
  @Owner(developers = ALEXEI)
  @RealMongo
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestExceptionWhenNoNodesAreFound() {
    PlanExecution planExecution = PlanExecution.builder().uuid("plan_test_id").build();
    planExecutionService.save(planExecution);

    assertThatThrownBy(() -> graphGenerationService.generateGraph(planExecution.getUuid()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("No nodes found for planExecutionId [" + planExecution.getUuid() + "]");
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestWithoutCache() {
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().build());
    StepParameters forkStepParams =
        DummyForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork = NodeExecution.builder()
                             .uuid("node1")
                             .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                             .mode(ExecutionMode.CHILDREN)
                             .node(PlanNode.builder()
                                       .uuid("node1_plan")
                                       .name("name1")
                                       .stepType(StepType.builder().type("DUMMY_FORK").build())
                                       .identifier("identifier1")
                                       .stepParameters(forkStepParams)
                                       .build())
                             .resolvedStepParameters(forkStepParams)
                             .build();
    NodeExecution parallelNode1 = NodeExecution.builder()
                                      .uuid("parallel_node_1")
                                      .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                                      .mode(ExecutionMode.SYNC)
                                      .node(PlanNode.builder()
                                                .uuid("parallel_plan_node_1")
                                                .name("name_children_1")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .identifier("name_children_1")
                                                .build())
                                      .parentId(fork.getUuid())
                                      .build();
    NodeExecution parallelNode2 = NodeExecution.builder()
                                      .uuid("parallel_node_2")
                                      .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                                      .mode(ExecutionMode.SYNC)
                                      .node(PlanNode.builder()
                                                .uuid("parallel_plan_node_2")
                                                .name("name_children_2")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .identifier("name_children_2")
                                                .build())
                                      .parentId(fork.getUuid())
                                      .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(fork, parallelNode1, parallelNode2);
    nodeExecutionRepository.saveAll(nodeExecutions);

    Graph graph = graphGenerationService.generateGraph(planExecution.getUuid());
    assertThat(graph).isNotNull();
    assertThat(graph.getGraphVertex()).isNotNull();
    assertThat(graph.getGraphVertex().getUuid()).isEqualTo(fork.getUuid());
    assertThat(graph.getGraphVertex().getNext()).isNull();

    assertThat(graph.getGraphVertex().getSubgraph()).isNotNull();
    assertThat(graph.getGraphVertex()
                   .getSubgraph()
                   .getVertices()
                   .stream()
                   .map(GraphVertex::getUuid)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(parallelNode1.getUuid(), parallelNode2.getUuid());

    Graph cachedGraph =
        mongoStore.get(Graph.ALGORITHM_ID, Graph.STRUCTURE_HASH, planExecution.getUuid(), Collections.emptyList());
    assertThat(cachedGraph).isNotNull();
    assertThat(cachedGraph.structureHash()).isEqualTo(Graph.STRUCTURE_HASH);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestWithCache() {
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().build());
    NodeExecution dummyStart = NodeExecution.builder()
                                   .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid("node1_plan")
                                             .name("name")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("identifier1")
                                             .build())
                                   .build();
    nodeExecutionRepository.save(dummyStart);

    GraphVertex vertex = GraphVertex.builder().uuid(dummyStart.getUuid()).name(dummyStart.getNode().getName()).build();

    Graph graph = Graph.builder()
                      .cacheContextOrder(System.currentTimeMillis())
                      .cacheKey(planExecution.getUuid())
                      .graphVertex(vertex)
                      .status(Status.SUCCEEDED)
                      .build();
    mongoStore.upsert(graph, Duration.ofDays(10));

    Graph graphResponse = graphGenerationService.generateGraph(planExecution.getUuid());
    assertThat(graphResponse).isNotNull();
    assertThat(graphResponse.getGraphVertex()).isNotNull();
    assertThat(graphResponse.getGraphVertex().getUuid()).isEqualTo(vertex.getUuid());
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

    OrchestrationGraph graphResponse = graphGenerationService.generateOrchestrationGraph(planExecution.getUuid());
    assertThat(graphResponse).isNotNull();
    assertThat(graphResponse.getAdjacencyList()).isNotNull();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap()).isNotEmpty();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(1);
    assertThat(graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)))
        .isNotNull();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)).getNextIds())
        .isEmpty();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)).getEdges())
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

    OrchestrationAdjacencyList adjacencyListInternal =
        OrchestrationAdjacencyList.builder()
            .graphVertexMap(ImmutableMap.of(dummyStart.getUuid(), GraphVertexConverter.convertFrom(dummyStart)))
            .adjacencyList(ImmutableMap.of(
                dummyStart.getUuid(), EdgeList.builder().edges(new ArrayList<>()).nextIds(new ArrayList<>()).build()))
            .build();
    OrchestrationGraphInternal graphInternal = OrchestrationGraphInternal.builder()
                                                   .rootNodeIds(Lists.newArrayList(dummyStart.getUuid()))
                                                   .cacheKey(planExecution.getUuid())
                                                   .adjacencyList(adjacencyListInternal)
                                                   .build();
    mongoStore.upsert(graphInternal, Duration.ofDays(10));

    OrchestrationGraph graphResponse = graphGenerationService.generateOrchestrationGraph(planExecution.getUuid());
    assertThat(graphResponse).isNotNull();
    assertThat(graphResponse.getAdjacencyList()).isNotNull();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap()).isNotEmpty();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(1);
    assertThat(graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)))
        .isNotNull();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)).getNextIds())
        .isEmpty();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)).getEdges())
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
    Map<String, EdgeList> adjacencyListMap = new HashMap<>();
    adjacencyListMap.put(
        dummyStart.getUuid(), EdgeList.builder().edges(new ArrayList<>()).nextIds(new ArrayList<>()).build());

    OrchestrationAdjacencyList adjacencyListInternal =
        OrchestrationAdjacencyList.builder().graphVertexMap(graphVertexMap).adjacencyList(adjacencyListMap).build();
    OrchestrationGraphInternal graphInternal = OrchestrationGraphInternal.builder()
                                                   .rootNodeIds(Lists.newArrayList(dummyStart.getUuid()))
                                                   .cacheKey(planExecution.getUuid())
                                                   .adjacencyList(adjacencyListInternal)
                                                   .build();
    mongoStore.upsert(graphInternal, Duration.ofDays(10));

    OrchestrationGraph graphResponse = graphGenerationService.generateOrchestrationGraph(planExecution.getUuid());
    assertThat(graphResponse).isNotNull();

    OrchestrationAdjacencyList orchestrationAdjacencyList = graphResponse.getAdjacencyList();
    assertThat(orchestrationAdjacencyList).isNotNull();

    Map<String, GraphVertex> graphVertexMapResponse = orchestrationAdjacencyList.getGraphVertexMap();
    assertThat(graphVertexMapResponse).isNotEmpty();
    assertThat(graphVertexMapResponse.size()).isEqualTo(2);

    Map<String, EdgeList> adjacencyList = orchestrationAdjacencyList.getAdjacencyList();
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

    OrchestrationGraph graphResponse = graphGenerationService.generatePartialOrchestrationGraph(
        dummyFinish.getNode().getUuid(), planExecution.getUuid());
    assertThat(graphResponse).isNotNull();
    assertThat(graphResponse.getRootNodeIds().get(0)).isEqualTo(dummyFinish.getUuid());
    assertThat(graphResponse.getAdjacencyList()).isNotNull();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap()).isNotEmpty();
    assertThat(graphResponse.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(1);
    assertThat(graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)))
        .isNotNull();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)).getNextIds())
        .isEmpty();
    assertThat(
        graphResponse.getAdjacencyList().getAdjacencyList().get(graphResponse.getRootNodeIds().get(0)).getEdges())
        .isEmpty();
  }

  private static Answer<?> executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }
}
