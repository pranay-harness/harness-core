package io.harness.steps.resourcerestraint;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.execution.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.OrchestrationStepsTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintId;
import io.harness.distribution.constraint.Consumer;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.pms.ambiance.Level;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;
import io.harness.steps.resourcerestraint.beans.HoldingScope.HoldingScopeBuilder;
import io.harness.steps.resourcerestraint.beans.ResourceConstraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.service.RestraintService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

public class ResourceRestraintStepTest extends OrchestrationStepsTestBase {
  private static final String CLAIMANT_ID = generateUuid();
  private static final String RESOURCE_RESTRAINT_ID = generateUuid();
  private static final String RESOURCE_UNIT = generateUuid();
  private static final HoldingScope HOLDING_SCOPE = HoldingScopeBuilder.aPlan().build();

  @Mock private ResourceRestraintService resourceRestraintService;
  @Mock private RestraintService restraintService;
  @Mock private EngineExpressionService engineExpressionService;
  @Inject @InjectMocks private ResourceRestraintRegistry resourceRestraintRegistry;
  @Inject @InjectMocks private ResourceRestraintStep resourceRestraintStep;

  @Before
  public void setUp() {
    ConstraintId constraintId = new ConstraintId(RESOURCE_RESTRAINT_ID);
    when(restraintService.get(any(), any()))
        .thenReturn(ResourceConstraint.builder()
                        .accountId(generateUuid())
                        .capacity(1)
                        .strategy(Constraint.Strategy.FIFO)
                        .uuid(generateUuid())
                        .build());
    doReturn(Constraint.builder()
                 .id(constraintId)
                 .spec(Constraint.Spec.builder().limits(1).strategy(Constraint.Strategy.FIFO).build())
                 .build())
        .when(resourceRestraintService)
        .createAbstraction(any());
    doReturn(ResourceRestraintInstance.builder().build()).when(resourceRestraintService).save(any());
    when(engineExpressionService.renderExpression(any(), any())).thenReturn(RESOURCE_UNIT);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteAsync() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    HoldingScope holdingScope = HoldingScopeBuilder.aPlan().build();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ACCUMULATE)
                                                         .holdingScope(holdingScope)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    doReturn(Collections.singletonList(ResourceRestraintInstance.builder()
                                           .state(Consumer.State.ACTIVE)
                                           .permits(1)
                                           .releaseEntityType(holdingScope.getScope())
                                           .releaseEntityId(holdingScope.getNodeSetupId())
                                           .build()))
        .when(resourceRestraintService)
        .getAllByRestraintIdAndResourceUnitAndStates(any(), any(), any());
    AsyncExecutableResponse asyncExecutableResponse =
        resourceRestraintStep.executeAsync(ambiance, stepParameters, stepInputPackage);

    assertThat(asyncExecutableResponse).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteAsync_InvalidRequestException() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ACCUMULATE)
                                                         .holdingScope(HOLDING_SCOPE)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    doReturn(Collections.emptyList())
        .when(resourceRestraintService)
        .getAllByRestraintIdAndResourceUnitAndStates(any(), any(), any());

    assertThatThrownBy(() -> resourceRestraintStep.executeAsync(ambiance, stepParameters, stepInputPackage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageStartingWith("The state should be BLOCKED");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteSync() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ACCUMULATE)
                                                         .holdingScope(HOLDING_SCOPE)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    doReturn(Collections.emptyList())
        .when(resourceRestraintService)
        .getAllByRestraintIdAndResourceUnitAndStates(any(), any(), any());

    StepResponse stepResponse = resourceRestraintStep.executeSync(ambiance, stepParameters, stepInputPackage, null);

    assertThat(stepResponse).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteSync_InvalidRequestException() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ACCUMULATE)
                                                         .holdingScope(HOLDING_SCOPE)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    doReturn(Collections.singletonList(ResourceRestraintInstance.builder()
                                           .state(Consumer.State.ACTIVE)
                                           .permits(1)
                                           .releaseEntityType(HOLDING_SCOPE.getScope())
                                           .releaseEntityId(HOLDING_SCOPE.getNodeSetupId())
                                           .build()))
        .when(resourceRestraintService)
        .getAllByRestraintIdAndResourceUnitAndStates(any(), any(), any());

    assertThatThrownBy(() -> resourceRestraintStep.executeSync(ambiance, stepParameters, stepInputPackage, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageStartingWith("The state should be ACTIVE");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestHandleAsyncResponse() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
            .build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ACCUMULATE)
                                                         .holdingScope(HOLDING_SCOPE)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    doNothing().when(resourceRestraintService).updateBlockedConstraints(any());

    StepResponse stepResponse = resourceRestraintStep.handleAsyncResponse(ambiance, stepParameters, null);

    assertThat(stepResponse).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(SUCCEEDED);

    verify(restraintService).get(any(), any());
    verify(resourceRestraintService).updateBlockedConstraints(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldHandleAbort() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
            .build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ACCUMULATE)
                                                         .holdingScope(HOLDING_SCOPE)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    when(resourceRestraintService.finishInstance(any(), any())).thenReturn(ResourceRestraintInstance.builder().build());

    resourceRestraintStep.handleAbort(
        ambiance, stepParameters, AsyncExecutableResponse.builder().callbackId(generateUuid()).build());

    verify(resourceRestraintService).finishInstance(any(), any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldHandleAbort_ThrowException() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
            .build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ACCUMULATE)
                                                         .holdingScope(HOLDING_SCOPE)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    assertThatThrownBy(()
                           -> resourceRestraintStep.handleAbort(
                               ambiance, stepParameters, AsyncExecutableResponse.builder().callbackId(null).build()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageStartingWith("CallbackId should not be null in handleAbort() for nodeExecution with id");

    verify(resourceRestraintService, never()).finishInstance(any(), any());
    verify(resourceRestraintService, never()).finishInstance(any(), any());
  }
}
