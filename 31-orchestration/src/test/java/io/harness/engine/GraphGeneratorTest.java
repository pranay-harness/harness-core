package io.harness.engine;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.presentation.GraphVertex;
import io.harness.rule.Owner;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.core.section.SectionStep;
import io.harness.state.core.section.SectionStepParameters;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.io.StepParameters;
import io.harness.utils.DummyOutcome;
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
public class GraphGeneratorTest extends OrchestrationTest {
  private static final String PLAN_EXECUTION_ID = "planId";
  private static final String STARTING_EXECUTION_NODE_ID = "startID";

  @Mock private OutcomeService outcomeService;
  @InjectMocks @Inject private GraphGenerator graphGenerator;

  @Before
  public void setUp() {
    when(outcomeService.findAllByRuntimeId(anyString(), anyString()))
        .thenReturn(Collections.singletonList(DummyOutcome.builder().test("outcome").build()));
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
                                   .planExecutionId(PLAN_EXECUTION_ID)
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid(STARTING_EXECUTION_NODE_ID)
                                             .name("name")
                                             .stepType(DummyStep.STEP_TYPE)
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
                                   .planExecutionId(PLAN_EXECUTION_ID)
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid(STARTING_EXECUTION_NODE_ID)
                                             .name("name")
                                             .stepType(DummyStep.STEP_TYPE)
                                             .identifier("identifier1")
                                             .build())
                                   .nextId("node2")
                                   .build();
    StepParameters sectionStepParams = SectionStepParameters.builder().childNodeId("child_section_2").build();
    NodeExecution section = NodeExecution.builder()
                                .uuid("node2")
                                .planExecutionId(PLAN_EXECUTION_ID)
                                .mode(ExecutionMode.CHILD)
                                .node(PlanNode.builder()
                                          .uuid("section_2")
                                          .name("name2")
                                          .stepType(SectionStep.STEP_TYPE)
                                          .identifier("identifier2")
                                          .stepParameters(sectionStepParams)
                                          .build())
                                .resolvedStepParameters(sectionStepParams)
                                .previousId(dummyStart.getUuid())
                                .build();
    NodeExecution sectionChild = NodeExecution.builder()
                                     .uuid("node_child_2")
                                     .planExecutionId(PLAN_EXECUTION_ID)
                                     .mode(ExecutionMode.SYNC)
                                     .node(PlanNode.builder()
                                               .uuid("child_section_2")
                                               .name("name_child_2")
                                               .stepType(DummyStep.STEP_TYPE)
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
        ForkStepParameters.builder().parallelNodeId("parallel_node_1").parallelNodeId("parallel_node_2").build();
    NodeExecution fork = NodeExecution.builder()
                             .uuid("node1")
                             .planExecutionId(PLAN_EXECUTION_ID)
                             .mode(ExecutionMode.CHILDREN)
                             .node(PlanNode.builder()
                                       .uuid(STARTING_EXECUTION_NODE_ID)
                                       .name("name1")
                                       .stepType(ForkStep.STEP_TYPE)
                                       .identifier("identifier1")
                                       .stepParameters(forkStepParams)
                                       .build())
                             .resolvedStepParameters(forkStepParams)
                             .build();
    NodeExecution parallelNode1 = NodeExecution.builder()
                                      .uuid("parallel_node_1")
                                      .planExecutionId(PLAN_EXECUTION_ID)
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
                                      .planExecutionId(PLAN_EXECUTION_ID)
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
                                               .planExecutionId(PLAN_EXECUTION_ID)
                                               .mode(ExecutionMode.CHILD_CHAIN)
                                               .node(PlanNode.builder()
                                                         .uuid("section_chain_plan_node")
                                                         .name("name_section_chain")
                                                         .identifier("name_section_chain")
                                                         .stepType(SectionChainStep.STEP_TYPE)
                                                         .build())
                                               .build();

    NodeExecution sectionChain1 = NodeExecution.builder()
                                      .uuid("section_chain_child1")
                                      .planExecutionId(PLAN_EXECUTION_ID)
                                      .mode(ExecutionMode.TASK)
                                      .node(PlanNode.builder()
                                                .uuid("section_chain_child1_plan_node")
                                                .name("name_section_chain_child1_plan_node")
                                                .identifier("name_section_chain_child1_plan_node")
                                                .stepType(DummyStep.STEP_TYPE)
                                                .build())
                                      .parentId(sectionChainParentNode.getUuid())
                                      .nextId(dummyNode1Uuid)
                                      .build();

    NodeExecution sectionChain2 = NodeExecution.builder()
                                      .uuid("section_chain_child2")
                                      .planExecutionId(PLAN_EXECUTION_ID)
                                      .mode(ExecutionMode.TASK)
                                      .node(PlanNode.builder()
                                                .uuid("section_chain_child2_plan_node")
                                                .name("name_section_chain_child2_plan_node")
                                                .identifier("name_section_chain_child2_plan_node")
                                                .stepType(DummyStep.STEP_TYPE)
                                                .build())
                                      .parentId(sectionChainParentNode.getUuid())
                                      .nextId(dummyNode2Uuid)
                                      .build();

    NodeExecution dummyNode1 = NodeExecution.builder()
                                   .uuid(dummyNode1Uuid)
                                   .planExecutionId(PLAN_EXECUTION_ID)
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid("dummy_plan_node_1")
                                             .name("name_dummy_node_1")
                                             .stepType(DummyStep.STEP_TYPE)
                                             .identifier("name_dummy_node_1")
                                             .build())
                                   .parentId(sectionChainParentNode.getUuid())
                                   .previousId(sectionChain1.getUuid())
                                   .build();

    NodeExecution dummyNode2 = NodeExecution.builder()
                                   .uuid(dummyNode2Uuid)
                                   .planExecutionId(PLAN_EXECUTION_ID)
                                   .mode(ExecutionMode.SYNC)
                                   .node(PlanNode.builder()
                                             .uuid("dummy_plan_node_2")
                                             .name("name_dummy_node_2")
                                             .stepType(DummyStep.STEP_TYPE)
                                             .identifier("name_dummy_node_2")
                                             .build())
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
}
