package io.harness.engine.facilitation;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.facilitation.facilitator.sync.SyncFacilitator;
import io.harness.execution.NodeExecution;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.PIPELINE)
public class FacilitationHelperTest extends OrchestrationTestBase {
  @Inject @InjectMocks private FacilitationHelper facilitationHelper;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFacilitateExecutionCore() {
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build();
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(ambiance)
            .status(Status.QUEUED)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .addFacilitatorObtainments(
                          FacilitatorObtainment.newBuilder().setType(SyncFacilitator.FACILITATOR_TYPE).build())
                      .build())
            .startTs(System.currentTimeMillis())
            .build();
    FacilitatorResponseProto facilitatorResponse = facilitationHelper.calculateFacilitatorResponse(nodeExecution);
    assertThat(facilitatorResponse.getExecutionMode()).isEqualTo(ExecutionMode.SYNC);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testCustomFacilitatorPresent() {
    boolean customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder().setType("CUSTOM").build())
                                       .build())
            .build());

    assertThat(customPresent).isTrue();

    customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build())
                    .build())
            .build());

    assertThat(customPresent).isFalse();

    customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build())
            .build());

    assertThat(customPresent).isFalse();

    customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                    .build())
            .build());

    assertThat(customPresent).isFalse();

    customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK_CHAIN).build())
                    .build())
            .build());

    assertThat(customPresent).isFalse();

    customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .build());

    assertThat(customPresent).isFalse();

    customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .build());

    assertThat(customPresent).isFalse();

    customPresent = facilitationHelper.customFacilitatorPresent(
        PlanNode.builder()
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .build());

    assertThat(customPresent).isFalse();
  }
}