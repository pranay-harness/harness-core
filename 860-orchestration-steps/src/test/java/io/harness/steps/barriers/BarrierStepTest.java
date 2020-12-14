package io.harness.steps.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.barrier.Barrier.State.DOWN;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationStepsTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rule.Owner;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.steps.barriers.service.BarrierService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class BarrierStepTest extends OrchestrationStepsTestBase {
  @Mock BarrierService barrierService;
  @Inject @InjectMocks BarrierStep barrierStep;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteSync() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    String barrierIdentifier = "barrierIdentifier";
    BarrierExecutionInstance barrier = BarrierExecutionInstance.builder()
                                           .uuid(uuid)
                                           .planNodeId(planNodeId)
                                           .identifier(barrierIdentifier)
                                           .barrierState(STANDING)
                                           .build();
    BarrierExecutionInstance updatedBarrier = BarrierExecutionInstance.builder()
                                                  .uuid(uuid)
                                                  .planNodeId(planNodeId)
                                                  .identifier(barrierIdentifier)
                                                  .barrierState(DOWN)
                                                  .build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
                            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    BarrierStepParameters stepParameters =
        BarrierStepParameters.builder().identifier(barrierIdentifier).timeoutInMillis(1000).build();

    when(barrierService.findByPlanNodeId(planNodeId)).thenReturn(barrier);
    when(barrierService.save(any())).thenReturn(updatedBarrier);

    StepResponse stepResponse = barrierStep.executeSync(ambiance, stepParameters, stepInputPackage, null);
    assertThat(stepResponse).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes().size()).isEqualTo(1);

    assertThat(updatedBarrier).isNotNull();
    assertThat(updatedBarrier.getBarrierState()).isEqualTo(DOWN);

    verify(barrierService).findByPlanNodeId(planNodeId);
    verify(barrierService).save(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteAsync() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    String barrierGroupId = generateUuid();
    String barrierIdentifier = "barrierIdentifier";
    BarrierExecutionInstance barrier = BarrierExecutionInstance.builder()
                                           .uuid(uuid)
                                           .planNodeId(planNodeId)
                                           .identifier(barrierIdentifier)
                                           .barrierState(STANDING)
                                           .barrierGroupId(barrierGroupId)
                                           .build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
                            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    BarrierStepParameters stepParameters =
        BarrierStepParameters.builder().identifier(barrierIdentifier).timeoutInMillis(1000).build();

    when(barrierService.findByPlanNodeId(planNodeId)).thenReturn(barrier);
    when(barrierService.update(barrier)).thenReturn(barrier);

    AsyncExecutableResponse stepResponse = barrierStep.executeAsync(ambiance, stepParameters, stepInputPackage);

    assertThat(stepResponse).isNotNull();
    assertThat(stepResponse.getCallbackIdsList()).contains(barrierGroupId);

    verify(barrierService).findByPlanNodeId(planNodeId);
    verify(barrierService).update(barrier);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponse() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    String barrierGroupId = generateUuid();
    String barrierIdentifier = "barrierIdentifier";
    BarrierExecutionInstance barrier = BarrierExecutionInstance.builder()
                                           .uuid(uuid)
                                           .planNodeId(planNodeId)
                                           .identifier(barrierIdentifier)
                                           .barrierState(STANDING)
                                           .barrierGroupId(barrierGroupId)
                                           .build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
                            .build();
    BarrierStepParameters stepParameters =
        BarrierStepParameters.builder().identifier(barrierIdentifier).timeoutInMillis(1000).build();

    when(barrierService.findByPlanNodeId(planNodeId)).thenReturn(barrier);
    when(barrierService.update(barrier)).thenReturn(barrier);

    StepResponse stepResponse = barrierStep.handleAsyncResponse(
        ambiance, stepParameters, ImmutableMap.of(barrierGroupId, BarrierResponseData.builder().failed(false).build()));

    assertThat(stepResponse).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);

    verify(barrierService).findByPlanNodeId(planNodeId);
    verify(barrierService).update(barrier);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldHandleAbort() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    String barrierIdentifier = "barrierIdentifier";
    BarrierExecutionInstance barrier = BarrierExecutionInstance.builder()
                                           .uuid(uuid)
                                           .planNodeId(planNodeId)
                                           .identifier(barrierIdentifier)
                                           .barrierState(STANDING)
                                           .build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
                            .build();
    BarrierStepParameters stepParameters =
        BarrierStepParameters.builder().identifier(barrierIdentifier).timeoutInMillis(1000).build();

    when(barrierService.findByPlanNodeId(planNodeId)).thenReturn(barrier);
    when(barrierService.update(barrier)).thenReturn(barrier);

    barrierStep.handleAbort(ambiance, stepParameters, null);

    verify(barrierService).findByPlanNodeId(planNodeId);
    verify(barrierService).update(barrier);
  }
}
