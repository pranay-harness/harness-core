package io.harness.steps.resourcerestraint;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.facilitator.modes.ExecutionMode.ASYNC;
import static io.harness.facilitator.modes.ExecutionMode.SYNC;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.OrchestrationStepsTest;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintId;
import io.harness.distribution.constraint.Consumer;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.rule.Owner;
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

public class ResourceRestraintFacilitatorTest extends OrchestrationStepsTest {
  private static final String CLAIMANT_ID = generateUuid();
  private static final String RESOURCE_RESTRAINT_ID = generateUuid();
  private static final String RESOURCE_UNIT = generateUuid();

  @Mock private ResourceRestraintService resourceRestraintService;
  @Mock private RestraintService restraintService;
  @Mock private EngineExpressionService engineExpressionService;
  @Inject @InjectMocks private ResourceRestraintRegistry resourceRestraintRegistry;
  @Inject @InjectMocks private ResourceRestraintFacilitator resourceRestraintFacilitator;

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
    when(engineExpressionService.renderExpression(any(), any())).thenReturn(RESOURCE_UNIT);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnAsyncMode() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    HoldingScope holdingScope = HoldingScopeBuilder.aPlan().build();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.builder().runtimeId(uuid).setupId(planNodeId).build()))
            .build();
    FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
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

    FacilitatorResponse response = resourceRestraintFacilitator.facilitate(ambiance, stepParameters, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(ASYNC);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnSyncMode() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    HoldingScope holdingScope = HoldingScopeBuilder.aPlan().build();
    Ambiance ambiance =
        Ambiance.builder()
            .planExecutionId(generateUuid())
            .levels(Collections.singletonList(Level.builder().runtimeId(uuid).setupId(planNodeId).build()))
            .build();
    FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceRestraintId(RESOURCE_RESTRAINT_ID)
                                                         .resourceUnit(RESOURCE_UNIT)
                                                         .acquireMode(AcquireMode.ENSURE)
                                                         .holdingScope(holdingScope)
                                                         .permits(1)
                                                         .claimantId(CLAIMANT_ID)
                                                         .build();

    doReturn(Collections.emptyList())
        .when(resourceRestraintService)
        .getAllByRestraintIdAndResourceUnitAndStates(any(), any(), any());
    doReturn(0).when(resourceRestraintService).getAllCurrentlyAcquiredPermits(any(), any());
    FacilitatorResponse response = resourceRestraintFacilitator.facilitate(ambiance, stepParameters, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(SYNC);
  }
}
