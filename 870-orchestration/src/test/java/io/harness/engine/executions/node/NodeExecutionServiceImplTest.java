package io.harness.engine.executions.node;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.ERRORED;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;
import io.harness.utils.AmbianceTestUtils;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class NodeExecutionServiceImplTest extends OrchestrationTestBase {
  @Inject @InjectMocks private NodeExecutionService nodeExecutionService;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSave() {
    String nodeExecutionId = generateUuid();
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(nodeExecutionId)
                                      .ambiance(AmbianceTestUtils.buildAmbiance())
                                      .node(PlanNodeProto.newBuilder()
                                                .setUuid(generateUuid())
                                                .setName("name")
                                                .setIdentifier("dummy")
                                                .setStepType(StepType.newBuilder().setType("DUMMY").build())
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(Status.QUEUED)
                                      .build();
    NodeExecution savedExecution = nodeExecutionService.save(nodeExecution);
    assertThat(savedExecution.getUuid()).isEqualTo(nodeExecutionId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestErrorOutNodes() {
    String nodeExecutionId1 = generateUuid();
    String nodeExecutionId2 = generateUuid();
    NodeExecution nodeExecution1 = NodeExecution.builder()
                                       .uuid(nodeExecutionId1)
                                       .ambiance(AmbianceTestUtils.buildAmbiance())
                                       .node(PlanNodeProto.newBuilder()
                                                 .setUuid(generateUuid())
                                                 .setName("name")
                                                 .setIdentifier("dummy")
                                                 .setStepType(StepType.newBuilder().setType("DUMMY").build())
                                                 .build())
                                       .startTs(System.currentTimeMillis())
                                       .status(Status.RUNNING)
                                       .build();
    NodeExecution nodeExecution2 = NodeExecution.builder()
                                       .uuid(nodeExecutionId2)
                                       .ambiance(AmbianceTestUtils.buildAmbiance())
                                       .node(PlanNodeProto.newBuilder()
                                                 .setUuid(generateUuid())
                                                 .setName("name")
                                                 .setIdentifier("dummy")
                                                 .setStepType(StepType.newBuilder().setType("DUMMY").build())
                                                 .build())
                                       .startTs(System.currentTimeMillis())
                                       .status(Status.SUCCEEDED)
                                       .build();
    nodeExecutionService.save(nodeExecution1);
    nodeExecutionService.save(nodeExecution2);

    boolean res = nodeExecutionService.errorOutActiveNodes(AmbianceTestUtils.PLAN_EXECUTION_ID);
    assertThat(res).isTrue();
    NodeExecution ne1 = nodeExecutionService.get(nodeExecutionId1);
    assertThat(ne1).isNotNull();
    assertThat(ne1.getStatus()).isEqualTo(ERRORED);

    NodeExecution ne2 = nodeExecutionService.get(nodeExecutionId2);
    assertThat(ne2).isNotNull();
    assertThat(ne2.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestExtractChildExecutions() {
    NodeExecutionService service = spy(NodeExecutionServiceImpl.class);
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build();
    NodeExecution pipelineNode =
        NodeExecution.builder().uuid("pipelineNode").status(Status.RUNNING).ambiance(ambiance).version(1L).build();
    NodeExecution stageNode = NodeExecution.builder()
                                  .uuid("stageNode")
                                  .status(Status.RUNNING)
                                  .parentId(pipelineNode.getUuid())
                                  .ambiance(ambiance)
                                  .version(1L)
                                  .build();
    NodeExecution forkNode = NodeExecution.builder()
                                 .uuid("forkNode")
                                 .status(Status.RUNNING)
                                 .parentId(stageNode.getUuid())
                                 .ambiance(ambiance)
                                 .version(1L)
                                 .build();
    NodeExecution child1 = NodeExecution.builder()
                               .uuid("child1")
                               .status(Status.RUNNING)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .version(1L)
                               .build();
    NodeExecution child2 = NodeExecution.builder()
                               .uuid("child2")
                               .status(Status.RUNNING)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .version(1L)
                               .build();
    NodeExecution child3 = NodeExecution.builder()
                               .uuid("child3")
                               .status(Status.RUNNING)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .version(1L)
                               .build();

    doReturn(Arrays.asList(pipelineNode, stageNode, forkNode, child1, child2, child3))
        .when(service)
        .fetchNodeExecutionsWithoutOldRetriesAndStatusIn(any(), any());

    List<NodeExecution> stageChildList = service.findAllChildrenWithStatusIn(
        ambiance.getPlanExecutionId(), stageNode.getUuid(), EnumSet.of(Status.RUNNING), true);
    assertThat(stageChildList).isNotEmpty();
    assertThat(stageChildList).hasSize(5);
    assertThat(stageChildList).containsExactlyInAnyOrder(stageNode, forkNode, child1, child2, child3);

    List<NodeExecution> stageChildListWithoutParent = service.findAllChildrenWithStatusIn(
        ambiance.getPlanExecutionId(), stageNode.getUuid(), EnumSet.of(Status.RUNNING), false);
    assertThat(stageChildListWithoutParent).isNotEmpty();
    assertThat(stageChildListWithoutParent).hasSize(4);
    assertThat(stageChildListWithoutParent).containsExactlyInAnyOrder(forkNode, child1, child2, child3);

    List<NodeExecution> forkChildList = service.findAllChildrenWithStatusIn(
        ambiance.getPlanExecutionId(), forkNode.getUuid(), EnumSet.of(Status.RUNNING), true);
    assertThat(forkChildList).isNotEmpty();
    assertThat(forkChildList).hasSize(4);
    assertThat(forkChildList).containsExactlyInAnyOrder(forkNode, child1, child2, child3);
  }
}
