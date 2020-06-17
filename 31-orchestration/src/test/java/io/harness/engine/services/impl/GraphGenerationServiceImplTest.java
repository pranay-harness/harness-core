package io.harness.engine.services.impl;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationTest;
import io.harness.beans.EmbeddedUser;
import io.harness.cache.MongoStore;
import io.harness.category.element.UnitTests;
import io.harness.engine.services.GraphGenerationService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.engine.services.repositories.NodeExecutionRepository;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.presentation.Graph;
import io.harness.presentation.GraphVertex;
import io.harness.rule.Owner;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.io.StepParameters;
import io.harness.testlib.RealMongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Test class for {@link GraphGenerationServiceImpl}
 */
public class GraphGenerationServiceImplTest extends OrchestrationTest {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionRepository nodeExecutionRepository;
  @Inject private MongoStore mongoStore;
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
    PlanExecution planExecution = PlanExecution.builder().uuid("plan_test_id").createdBy(createdBy()).build();
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
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().createdBy(createdBy()).build());
    StepParameters forkStepParams =
        ForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork = NodeExecution.builder()
                             .uuid("node1")
                             .planExecutionId(planExecution.getUuid())
                             .mode(ExecutionMode.CHILDREN)
                             .node(PlanNode.builder()
                                       .uuid("node1_plan")
                                       .name("name1")
                                       .stepType(ForkStep.STEP_TYPE)
                                       .identifier("identifier1")
                                       .stepParameters(forkStepParams)
                                       .build())
                             .resolvedStepParameters(forkStepParams)
                             .build();
    NodeExecution parallelNode1 = NodeExecution.builder()
                                      .uuid("parallel_node_1")
                                      .planExecutionId(planExecution.getUuid())
                                      .mode(ExecutionMode.SYNC)
                                      .node(PlanNode.builder()
                                                .uuid("parallel_plan_node_1")
                                                .name("name_children_1")
                                                .stepType(DummyStep.STEP_TYPE)
                                                .identifier("name_children_1")
                                                .build())
                                      .parentId(fork.getUuid())
                                      .build();
    NodeExecution parallelNode2 = NodeExecution.builder()
                                      .uuid("parallel_node_2")
                                      .planExecutionId(planExecution.getUuid())
                                      .mode(ExecutionMode.SYNC)
                                      .node(PlanNode.builder()
                                                .uuid("parallel_plan_node_2")
                                                .name("name_children_2")
                                                .stepType(DummyStep.STEP_TYPE)
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
    PlanExecution planExecution = planExecutionService.save(PlanExecution.builder().createdBy(createdBy()).build());
    NodeExecution dummyStart = NodeExecution.builder()
                                   .planExecutionId(planExecution.getUuid())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid("node1_plan")
                                             .name("name")
                                             .stepType(DummyStep.STEP_TYPE)
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

  private static Answer<?> executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }

  private EmbeddedUser createdBy() {
    return EmbeddedUser.builder().uuid(ALEXEI + "whj983regf").name(ALEXEI).email(ALEXEI + "@harness.io").build();
  }
}
