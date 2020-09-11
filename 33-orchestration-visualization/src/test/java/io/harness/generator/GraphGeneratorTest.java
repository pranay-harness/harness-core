package io.harness.generator;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.OrchestrationVisualizationTest;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.category.element.UnitTests;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.utils.DummyForkStepParameters;
import io.harness.utils.DummyOutcome;
import io.harness.utils.DummySectionStepParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test class for {@link GraphGenerator}
 */
public class GraphGeneratorTest extends OrchestrationVisualizationTest {
  private static final String PLAN_EXECUTION_ID = "planId";
  private static final String STARTING_EXECUTION_NODE_ID = "startID";

  private static final StepType DUMMY_STEP_TYPE = StepType.builder().type("DUMMY").build();

  @Mock private OutcomeService outcomeService;
  @InjectMocks @Inject private GraphGenerator graphGenerator;

  @Before
  public void setUp() {
    when(outcomeService.findAllByRuntimeId(anyString(), anyString()))
        .thenReturn(Collections.singletonList(new DummyOutcome("outcome")));
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnNullWhenStartingNodeExIdIsNull() {
    assertThat(graphGenerator.generateGraphVertexStartingFrom(null, Collections.emptyList())).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowUnexpectedExceptionWhenNodeIsNotPresent() {
    NodeExecution dummyStart = NodeExecution.builder()
                                   .uuid("node1")
                                   .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid(STARTING_EXECUTION_NODE_ID)
                                             .name("name")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("identifier1")
                                             .build())
                                   .nextId("node2")
                                   .build();

    assertThatThrownBy(()
                           -> graphGenerator.generateGraphVertexStartingFrom(
                               dummyStart.getUuid(), Collections.singletonList(dummyStart)))
        .isInstanceOf(UnexpectedException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateGraphVertexWithSection() {
    NodeExecution dummyStart = NodeExecution.builder()
                                   .uuid("node1")
                                   .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid(STARTING_EXECUTION_NODE_ID)
                                             .name("name")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("identifier1")
                                             .build())
                                   .nextId("node2")
                                   .build();
    StepParameters sectionStepParams = DummySectionStepParameters.builder().childNodeId("child_section_2").build();
    NodeExecution section = NodeExecution.builder()
                                .uuid("node2")
                                .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                .mode(ExecutionMode.CHILD)
                                .node(PlanNode.builder()
                                          .uuid("section_2")
                                          .name("name2")
                                          .stepType(StepType.builder().type("DUMMY_SECTION").build())
                                          .identifier("identifier2")
                                          .stepParameters(sectionStepParams)
                                          .build())
                                .resolvedStepParameters(sectionStepParams)
                                .previousId(dummyStart.getUuid())
                                .build();
    NodeExecution sectionChild = NodeExecution.builder()
                                     .uuid("node_child_2")
                                     .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                     .mode(ExecutionMode.SYNC)
                                     .node(PlanNode.builder()
                                               .uuid("child_section_2")
                                               .name("name_child_2")
                                               .stepType(StepType.builder().type("DUMMY").build())
                                               .identifier("identifier_child_2")
                                               .build())
                                     .parentId(section.getUuid())
                                     .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(dummyStart, section, sectionChild);

    GraphVertex graphVertex = graphGenerator.generateGraphVertexStartingFrom(dummyStart.getUuid(), nodeExecutions);

    assertThat(graphVertex).isNotNull();
    assertThat(graphVertex.getUuid()).isEqualTo(dummyStart.getUuid());

    assertThat(graphVertex.getNext()).isNotNull();
    assertThat(graphVertex.getNext().getUuid()).isEqualTo(section.getUuid());

    assertThat(graphVertex.getNext().getSubgraph()).isNotNull();
    assertThat(graphVertex.getNext()
                   .getSubgraph()
                   .getVertices()
                   .stream()
                   .filter(vertex -> vertex.getUuid().equals(sectionChild.getUuid()))
                   .findAny())
        .isPresent();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateGraphWithFork() {
    StepParameters forkStepParams =
        DummyForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork = NodeExecution.builder()
                             .uuid("node1")
                             .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                             .mode(ExecutionMode.CHILDREN)
                             .node(PlanNode.builder()
                                       .uuid(STARTING_EXECUTION_NODE_ID)
                                       .name("name1")
                                       .stepType(StepType.builder().type("DUMMY_FORK").build())
                                       .identifier("identifier1")
                                       .stepParameters(forkStepParams)
                                       .build())
                             .resolvedStepParameters(forkStepParams)
                             .createdAt(System.currentTimeMillis())
                             .lastUpdatedAt(System.currentTimeMillis())
                             .build();
    NodeExecution parallelNode1 = NodeExecution.builder()
                                      .uuid("parallel_node_1")
                                      .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                      .mode(ExecutionMode.SYNC)
                                      .node(PlanNode.builder()
                                                .uuid("parallel_plan_node_1")
                                                .name("name_children_1")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .identifier("name_children_1")
                                                .build())
                                      .parentId(fork.getUuid())
                                      .createdAt(System.currentTimeMillis())
                                      .lastUpdatedAt(System.currentTimeMillis())
                                      .build();
    NodeExecution parallelNode2 = NodeExecution.builder()
                                      .uuid("parallel_node_2")
                                      .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                      .mode(ExecutionMode.SYNC)
                                      .node(PlanNode.builder()
                                                .uuid("parallel_plan_node_2")
                                                .name("name_children_2")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .identifier("name_children_2")
                                                .build())
                                      .parentId(fork.getUuid())
                                      .createdAt(System.currentTimeMillis())
                                      .lastUpdatedAt(System.currentTimeMillis())
                                      .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(fork, parallelNode1, parallelNode2);

    GraphVertex graphVertex = graphGenerator.generateGraphVertexStartingFrom(fork.getUuid(), nodeExecutions);

    assertThat(graphVertex).isNotNull();
    assertThat(graphVertex.getUuid()).isEqualTo(fork.getUuid());
    assertThat(graphVertex.getNext()).isNull();

    assertThat(graphVertex.getSubgraph()).isNotNull();
    assertThat(graphVertex.getSubgraph().getVertices().stream().map(GraphVertex::getUuid).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(parallelNode1.getUuid(), parallelNode2.getUuid());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateGraphWithChildChain() {
    String dummyNode1Uuid = "dummyNode1";
    String dummyNode2Uuid = "dummyNode2";

    NodeExecution sectionChainParentNode = NodeExecution.builder()
                                               .uuid("section_chain_start")
                                               .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                               .mode(ExecutionMode.CHILD_CHAIN)
                                               .node(PlanNode.builder()
                                                         .uuid("section_chain_plan_node")
                                                         .name("name_section_chain")
                                                         .identifier("name_section_chain")
                                                         .stepType(StepType.builder().type("SECTION_CHAIN").build())
                                                         .build())
                                               .createdAt(System.currentTimeMillis())
                                               .lastUpdatedAt(System.currentTimeMillis())
                                               .build();

    NodeExecution sectionChain1 = NodeExecution.builder()
                                      .uuid("section_chain_child1")
                                      .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                      .mode(ExecutionMode.TASK)
                                      .node(PlanNode.builder()
                                                .uuid("section_chain_child1_plan_node")
                                                .name("name_section_chain_child1_plan_node")
                                                .identifier("name_section_chain_child1_plan_node")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .build())
                                      .createdAt(System.currentTimeMillis())
                                      .lastUpdatedAt(System.currentTimeMillis())
                                      .parentId(sectionChainParentNode.getUuid())
                                      .nextId(dummyNode1Uuid)
                                      .build();

    NodeExecution sectionChain2 = NodeExecution.builder()
                                      .uuid("section_chain_child2")
                                      .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                      .mode(ExecutionMode.TASK)
                                      .node(PlanNode.builder()
                                                .uuid("section_chain_child2_plan_node")
                                                .name("name_section_chain_child2_plan_node")
                                                .identifier("name_section_chain_child2_plan_node")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .build())
                                      .createdAt(System.currentTimeMillis())
                                      .lastUpdatedAt(System.currentTimeMillis())
                                      .parentId(sectionChainParentNode.getUuid())
                                      .nextId(dummyNode2Uuid)
                                      .build();

    NodeExecution dummyNode1 = NodeExecution.builder()
                                   .uuid(dummyNode1Uuid)
                                   .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid("dummy_plan_node_1")
                                             .name("name_dummy_node_1")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("name_dummy_node_1")
                                             .build())
                                   .createdAt(System.currentTimeMillis())
                                   .lastUpdatedAt(System.currentTimeMillis())
                                   .parentId(sectionChainParentNode.getUuid())
                                   .previousId(sectionChain1.getUuid())
                                   .build();

    NodeExecution dummyNode2 = NodeExecution.builder()
                                   .uuid(dummyNode2Uuid)
                                   .ambiance(Ambiance.builder().planExecutionId(PLAN_EXECUTION_ID).build())
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid("dummy_plan_node_2")
                                             .name("name_dummy_node_2")
                                             .stepType(StepType.builder().type("DUMMY").build())
                                             .identifier("name_dummy_node_2")
                                             .build())
                                   .createdAt(System.currentTimeMillis())
                                   .lastUpdatedAt(System.currentTimeMillis())
                                   .parentId(sectionChainParentNode.getUuid())
                                   .previousId(sectionChain2.getUuid())
                                   .build();

    List<NodeExecution> nodeExecutions =
        Lists.newArrayList(sectionChainParentNode, sectionChain1, sectionChain2, dummyNode1, dummyNode2);

    GraphVertex graphVertex =
        graphGenerator.generateGraphVertexStartingFrom(sectionChainParentNode.getUuid(), nodeExecutions);

    assertThat(graphVertex).isNotNull();
    assertThat(graphVertex.getNext()).isNull();
    assertThat(graphVertex.getUuid()).isEqualTo(sectionChainParentNode.getUuid());

    assertThat(graphVertex.getSubgraph()).isNotNull();
    assertThat(graphVertex.getSubgraph().getMode()).isEqualTo(sectionChainParentNode.getMode());
    assertThat(graphVertex.getSubgraph().getVertices().size()).isEqualTo(1);

    GraphVertex sectionChain1Vertex = graphVertex.getSubgraph().getVertices().get(0);
    assertThat(sectionChain1Vertex).isNotNull();
    assertThat(sectionChain1Vertex.getUuid()).isEqualTo(sectionChain1.getUuid());
    assertThat(sectionChain1Vertex.getSubgraph()).isNull();
    assertThat(sectionChain1Vertex.getNext()).isNotNull();

    GraphVertex sectionChain1ChildVertex = sectionChain1Vertex.getNext();
    assertThat(sectionChain1ChildVertex).isNotNull();
    assertThat(sectionChain1ChildVertex.getUuid()).isEqualTo(dummyNode1.getUuid());
    assertThat(sectionChain1ChildVertex.getSubgraph()).isNull();
    assertThat(sectionChain1ChildVertex.getNext()).isNotNull();

    GraphVertex sectionChain2Vertex = sectionChain1ChildVertex.getNext();
    assertThat(sectionChain2Vertex).isNotNull();
    assertThat(sectionChain2Vertex.getUuid()).isEqualTo(sectionChain2.getUuid());
    assertThat(sectionChain2Vertex.getSubgraph()).isNull();
    assertThat(sectionChain2Vertex.getNext()).isNotNull();

    GraphVertex sectionChain2ChildVertex = sectionChain2Vertex.getNext();
    assertThat(sectionChain2ChildVertex).isNotNull();
    assertThat(sectionChain2ChildVertex.getUuid()).isEqualTo(dummyNode2.getUuid());
    assertThat(sectionChain2ChildVertex.getSubgraph()).isNull();
    assertThat(sectionChain2ChildVertex.getNext()).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateOrchestrationAdjacencyListWithSection() {
    NodeExecution dummyStart =
        NodeExecution.builder()
            .uuid("node1")
            .ambiance(
                Ambiance.builder()
                    .planExecutionId(PLAN_EXECUTION_ID)
                    .levels(Collections.singletonList(Level.builder().setupId(STARTING_EXECUTION_NODE_ID).build()))
                    .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid(STARTING_EXECUTION_NODE_ID)
                      .name("name")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("identifier1")
                      .build())
            .nextId("node2")
            .build();
    StepParameters sectionStepParams = DummySectionStepParameters.builder().childNodeId("child_section_2").build();
    NodeExecution section =
        NodeExecution.builder()
            .uuid("node2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("section_2").build()))
                          .build())
            .mode(ExecutionMode.CHILD)
            .node(PlanNode.builder()
                      .uuid("section_2")
                      .name("name2")
                      .stepType(StepType.builder().type("SECTION").build())
                      .identifier("identifier2")
                      .stepParameters(sectionStepParams)
                      .build())
            .resolvedStepParameters(sectionStepParams)
            .previousId(dummyStart.getUuid())
            .build();
    NodeExecution sectionChild =
        NodeExecution.builder()
            .uuid("node_child_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("child_section_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("child_section_2")
                      .name("name_child_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("identifier_child_2")
                      .build())
            .parentId(section.getUuid())
            .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(dummyStart, section, sectionChild);

    OrchestrationAdjacencyList adjacencyList =
        graphGenerator.generateAdjacencyList(dummyStart.getUuid(), nodeExecutions);

    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(3);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(dummyStart.getUuid(), section.getUuid(), sectionChild.getUuid());

    assertThat(adjacencyList.getAdjacencyList().size()).isEqualTo(3);
    assertThat(adjacencyList.getAdjacencyList().get(dummyStart.getUuid()).getNext()).isEqualTo(section.getUuid());
    assertThat(adjacencyList.getAdjacencyList().get(section.getUuid()).getEdges())
        .containsExactlyInAnyOrder(sectionChild.getUuid());
    assertThat(adjacencyList.getAdjacencyList().get(sectionChild.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyList().get(sectionChild.getUuid()).getNext()).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateOrchestrationAdjacencyListWithChildChain() {
    String dummyNode1Uuid = "dummyNode1";
    String dummyNode2Uuid = "dummyNode2";

    NodeExecution sectionChainParentNode =
        NodeExecution.builder()
            .uuid("section_chain_start")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("section_chain_plan_node").build()))
                          .build())
            .mode(ExecutionMode.CHILD_CHAIN)
            .node(PlanNode.builder()
                      .uuid("section_chain_plan_node")
                      .name("name_section_chain")
                      .identifier("name_section_chain")
                      .stepType(StepType.builder().type("_DUMMY_SECTION_CHAIN").build())
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();

    NodeExecution sectionChain1 =
        NodeExecution.builder()
            .uuid("section_chain_child1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(
                              Level.builder().setupId("section_chain_child1_plan_node").build()))
                          .build())
            .mode(ExecutionMode.TASK)
            .node(PlanNode.builder()
                      .uuid("section_chain_child1_plan_node")
                      .name("name_section_chain_child1_plan_node")
                      .identifier("name_section_chain_child1_plan_node")
                      .stepType(DUMMY_STEP_TYPE)
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .nextId(dummyNode1Uuid)
            .build();

    NodeExecution sectionChain2 =
        NodeExecution.builder()
            .uuid("section_chain_child2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(
                              Level.builder().setupId("section_chain_child2_plan_node").build()))
                          .build())
            .mode(ExecutionMode.TASK)
            .node(PlanNode.builder()
                      .uuid("section_chain_child2_plan_node")
                      .name("name_section_chain_child2_plan_node")
                      .identifier("name_section_chain_child2_plan_node")
                      .stepType(DUMMY_STEP_TYPE)
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .nextId(dummyNode2Uuid)
            .build();

    NodeExecution dummyNode1 =
        NodeExecution.builder()
            .uuid(dummyNode1Uuid)
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_1")
                      .name("name_dummy_node_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_dummy_node_1")
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .previousId(sectionChain1.getUuid())
            .build();

    NodeExecution dummyNode2 =
        NodeExecution.builder()
            .uuid(dummyNode2Uuid)
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_2")
                      .name("name_dummy_node_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_dummy_node_2")
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentId(sectionChainParentNode.getUuid())
            .previousId(sectionChain2.getUuid())
            .build();

    List<NodeExecution> nodeExecutions =
        Lists.newArrayList(sectionChainParentNode, sectionChain1, sectionChain2, dummyNode1, dummyNode2);

    OrchestrationAdjacencyList adjacencyList =
        graphGenerator.generateAdjacencyList(sectionChainParentNode.getUuid(), nodeExecutions);
    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(5);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(sectionChainParentNode.getUuid(), sectionChain1.getUuid(), sectionChain2.getUuid(),
            dummyNode1.getUuid(), dummyNode2.getUuid());

    assertThat(adjacencyList.getAdjacencyList().size()).isEqualTo(5);
    assertThat(adjacencyList.getAdjacencyList().get(sectionChainParentNode.getUuid()).getEdges())
        .containsExactlyInAnyOrder(sectionChain1.getUuid());
    assertThat(adjacencyList.getAdjacencyList().get(sectionChain1.getUuid()).getNext()).isEqualTo(dummyNode1.getUuid());
    assertThat(adjacencyList.getAdjacencyList().get(dummyNode1.getUuid()).getNext()).isEqualTo(sectionChain2.getUuid());
    assertThat(adjacencyList.getAdjacencyList().get(sectionChain2.getUuid()).getNext()).isEqualTo(dummyNode2.getUuid());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGenerateOrchestrationGraphWithFork() {
    StepParameters forkStepParams =
        DummyForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork =
        NodeExecution.builder()
            .uuid("node1")
            .ambiance(
                Ambiance.builder()
                    .planExecutionId(PLAN_EXECUTION_ID)
                    .levels(Collections.singletonList(Level.builder().setupId(STARTING_EXECUTION_NODE_ID).build()))
                    .build())
            .mode(ExecutionMode.CHILDREN)
            .node(PlanNode.builder()
                      .uuid(STARTING_EXECUTION_NODE_ID)
                      .name("name1")
                      .stepType(StepType.builder().type("DUMMY_FORK").build())
                      .identifier("identifier1")
                      .stepParameters(forkStepParams)
                      .build())
            .resolvedStepParameters(forkStepParams)
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode1 =
        NodeExecution.builder()
            .uuid("parallel_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_1")
                      .name("name_children_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_1")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode2 =
        NodeExecution.builder()
            .uuid("parallel_node_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_2")
                      .name("name_children_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_2")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(fork, parallelNode1, parallelNode2);

    OrchestrationAdjacencyList adjacencyList = graphGenerator.generateAdjacencyList(fork.getUuid(), nodeExecutions);
    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(3);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(fork.getUuid(), parallelNode1.getUuid(), parallelNode2.getUuid());

    assertThat(adjacencyList.getAdjacencyList().get(fork.getUuid())).isNotNull();
    assertThat(adjacencyList.getAdjacencyList().get(fork.getUuid()).getNext()).isNull();
    assertThat(adjacencyList.getAdjacencyList().get(fork.getUuid()).getEdges())
        .containsExactlyInAnyOrder(parallelNode1.getUuid(), parallelNode2.getUuid());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGeneratePartialOrchestrationGraphWithFork() {
    NodeExecution dummyNode1 =
        NodeExecution.builder()
            .uuid("dummy_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("dummy_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("dummy_plan_node_1")
                      .name("dummy_node_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("dummy_node_1")
                      .build())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    StepParameters forkStepParams =
        DummyForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork =
        NodeExecution.builder()
            .uuid("node1")
            .ambiance(
                Ambiance.builder()
                    .planExecutionId(PLAN_EXECUTION_ID)
                    .levels(Collections.singletonList(Level.builder().setupId(STARTING_EXECUTION_NODE_ID).build()))
                    .build())
            .mode(ExecutionMode.CHILDREN)
            .node(PlanNode.builder()
                      .uuid(STARTING_EXECUTION_NODE_ID)
                      .name("name1")
                      .stepType(StepType.builder().type("DUMMY_FORK").build())
                      .identifier("identifier1")
                      .stepParameters(forkStepParams)
                      .build())
            .resolvedStepParameters(forkStepParams)
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode1 =
        NodeExecution.builder()
            .uuid("parallel_node_1")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_1").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_1")
                      .name("name_children_1")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_1")
                      .build())
            .parentId(fork.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    NodeExecution parallelNode2 =
        NodeExecution.builder()
            .uuid("parallel_node_2")
            .ambiance(Ambiance.builder()
                          .planExecutionId(PLAN_EXECUTION_ID)
                          .levels(Collections.singletonList(Level.builder().setupId("parallel_plan_node_2").build()))
                          .build())
            .mode(ExecutionMode.SYNC)
            .node(PlanNode.builder()
                      .uuid("parallel_plan_node_2")
                      .name("name_children_2")
                      .stepType(DUMMY_STEP_TYPE)
                      .identifier("name_children_2")
                      .build())
            .parentId(fork.getUuid())
            .nextId(dummyNode1.getUuid())
            .createdAt(System.currentTimeMillis())
            .lastUpdatedAt(System.currentTimeMillis())
            .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(fork, parallelNode1, parallelNode2, dummyNode1);

    OrchestrationAdjacencyList adjacencyList =
        graphGenerator.generateAdjacencyList(parallelNode2.getUuid(), nodeExecutions);
    assertThat(adjacencyList).isNotNull();

    assertThat(adjacencyList.getGraphVertexMap().size()).isEqualTo(2);
    assertThat(adjacencyList.getGraphVertexMap().keySet())
        .containsExactlyInAnyOrder(parallelNode2.getUuid(), dummyNode1.getUuid());

    assertThat(adjacencyList.getAdjacencyList().get(parallelNode2.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyList().get(parallelNode2.getUuid()).getNext()).isEqualTo(dummyNode1.getUuid());

    assertThat(adjacencyList.getAdjacencyList().get(dummyNode1.getUuid()).getEdges()).isEmpty();
    assertThat(adjacencyList.getAdjacencyList().get(dummyNode1.getUuid()).getNext()).isNull();
  }
}
