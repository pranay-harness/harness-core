package io.harness.engine;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.engine.services.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.plan.PlanNode;
import io.harness.resource.GraphVertex;
import io.harness.rule.Owner;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.core.section.SectionStep;
import io.harness.state.core.section.SectionStepParameters;
import io.harness.state.io.StepParameters;
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

  @Mock private NodeExecutionService nodeExecutionService;
  @InjectMocks @Inject private GraphGenerator graphGenerator;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnNullWhenNoNodesFound() {
    when(nodeExecutionService.fetchNodeExecutions(null)).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> graphGenerator.generateGraphVertex(null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("No nodes found for planExecutionId [null]");
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

    when(nodeExecutionService.fetchNodeExecutions(PLAN_EXECUTION_ID)).thenReturn(Collections.singletonList(dummyStart));

    assertThatThrownBy(() -> graphGenerator.generateGraphVertex(PLAN_EXECUTION_ID))
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

    when(nodeExecutionService.fetchNodeExecutions(PLAN_EXECUTION_ID)).thenReturn(nodeExecutions);

    GraphVertex graphVertex = graphGenerator.generateGraphVertex(PLAN_EXECUTION_ID);

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
                                                .uuid("parallel_node_1")
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
                                                .uuid("parallel_node_2")
                                                .name("name_children_2")
                                                .stepType(DummyStep.STEP_TYPE)
                                                .identifier("name_children_2")
                                                .build())
                                      .parentId(fork.getUuid())
                                      .build();
    List<NodeExecution> nodeExecutions = Lists.newArrayList(fork, parallelNode1, parallelNode2);

    when(nodeExecutionService.fetchNodeExecutions(PLAN_EXECUTION_ID)).thenReturn(nodeExecutions);

    GraphVertex graphVertex = graphGenerator.generateGraphVertex(PLAN_EXECUTION_ID);

    assertThat(graphVertex).isNotNull();
    assertThat(graphVertex.getUuid()).isEqualTo(fork.getUuid());
    assertThat(graphVertex.getNext()).isNull();

    assertThat(graphVertex.getSubgraph()).isNotNull();
    assertThat(graphVertex.getSubgraph().getVertices().stream().map(GraphVertex::getUuid).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(parallelNode1.getUuid(), parallelNode2.getUuid());
  }
}
