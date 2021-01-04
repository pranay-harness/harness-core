package io.harness.steps.resourcerestraint;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.ExecutionMode.ASYNC;
import static io.harness.pms.contracts.execution.ExecutionMode.SYNC;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationStepsTestBase;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintId;
import io.harness.distribution.constraint.Consumer;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.facilitator.DefaultFacilitatorParams;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;
import io.harness.steps.resourcerestraint.beans.HoldingScope.HoldingScopeBuilder;
import io.harness.steps.resourcerestraint.beans.ResourceConstraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.service.RestraintService;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ResourceRestraintFacilitatorTest extends OrchestrationStepsTestBase {
  private static final String CLAIMANT_ID = generateUuid();
  private static final String RESOURCE_RESTRAINT_ID = generateUuid();
  private static final String RESOURCE_UNIT = generateUuid();

  @Inject private KryoSerializer kryoSerializer;
  @Mock private ResourceRestraintService resourceRestraintService;
  @Mock private RestraintService restraintService;
  @Mock private PmsEngineExpressionService pmsEngineExpressionService;
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
    when(pmsEngineExpressionService.renderExpression(any(), any())).thenReturn(RESOURCE_UNIT);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnAsyncMode() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    HoldingScope holdingScope = HoldingScopeBuilder.aPlan().build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(generateUuid())
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
                            .build();
    byte[] parameters = kryoSerializer.asBytes(DefaultFacilitatorParams.builder().build());
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
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(generateUuid())
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(uuid).setSetupId(planNodeId).build()))
                            .build();
    byte[] parameters = kryoSerializer.asBytes(DefaultFacilitatorParams.builder().build());
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
