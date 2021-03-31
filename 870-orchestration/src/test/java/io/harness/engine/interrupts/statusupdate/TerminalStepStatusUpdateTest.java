package io.harness.engine.interrupts.statusupdate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class TerminalStepStatusUpdateTest extends OrchestrationTestBase {
  @Inject @InjectMocks TerminalStepStatusUpdate stepStatusUpdate;
  @Mock NodeExecutionService nodeExecutionService;
  @Mock PlanExecutionService planExecutionService;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStatusUpdateIntervention() {
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
    // This node is getting transitioned to SUCCEEDED from MANUAL_INTERVENTION
    NodeExecution child1 = NodeExecution.builder()
                               .uuid("child1")
                               .status(Status.SUCCEEDED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .version(1L)
                               .build();
    NodeExecution child2 = NodeExecution.builder()
                               .uuid("child2")
                               .status(Status.INTERVENTION_WAITING)
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
        .when(nodeExecutionService)
        .fetchNodeExecutionsWithoutOldRetriesAndStatusIn(any(), eq(StatusUtils.activeStatuses()));

    stepStatusUpdate.onStepStatusUpdate(StepStatusUpdateInfo.builder()
                                            .interruptId(generateUuid())
                                            .nodeExecutionId("child1")
                                            .planExecutionId(ambiance.getPlanExecutionId())
                                            .status(Status.SUCCEEDED)
                                            .build());

    verify(planExecutionService, times(1))
        .updateStatus(eq(ambiance.getPlanExecutionId()), eq(Status.INTERVENTION_WAITING));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStatusUpdateRunning() {
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
    // This node is getting transitioned to SUCCEEDED from MANUAL_INTERVENTION
    NodeExecution child1 = NodeExecution.builder()
                               .uuid("child1")
                               .status(Status.SUCCEEDED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .version(1L)
                               .build();
    NodeExecution child2 = NodeExecution.builder()
                               .uuid("child2")
                               .status(Status.SUCCEEDED)
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
        .when(nodeExecutionService)
        .fetchNodeExecutionsWithoutOldRetriesAndStatusIn(any(), eq(StatusUtils.activeStatuses()));

    stepStatusUpdate.onStepStatusUpdate(StepStatusUpdateInfo.builder()
                                            .interruptId(generateUuid())
                                            .nodeExecutionId("child1")
                                            .planExecutionId(ambiance.getPlanExecutionId())
                                            .status(Status.SUCCEEDED)
                                            .build());

    verify(planExecutionService, times(1)).updateStatus(eq(ambiance.getPlanExecutionId()), eq(Status.RUNNING));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStatusUpdateApprovalWaiting() {
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
    // This node is getting transitioned to SUCCEEDED from MANUAL_INTERVENTION
    NodeExecution child1 = NodeExecution.builder()
                               .uuid("child1")
                               .status(Status.FAILED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .version(1L)
                               .build();
    NodeExecution child2 = NodeExecution.builder()
                               .uuid("child2")
                               .status(Status.APPROVAL_WAITING)
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
        .when(nodeExecutionService)
        .fetchNodeExecutionsWithoutOldRetriesAndStatusIn(any(), eq(StatusUtils.activeStatuses()));

    stepStatusUpdate.onStepStatusUpdate(StepStatusUpdateInfo.builder()
                                            .interruptId(generateUuid())
                                            .nodeExecutionId("child1")
                                            .planExecutionId(ambiance.getPlanExecutionId())
                                            .status(Status.SUCCEEDED)
                                            .build());

    verify(planExecutionService, times(1)).updateStatus(eq(ambiance.getPlanExecutionId()), eq(Status.APPROVAL_WAITING));
  }
}
