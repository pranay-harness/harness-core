package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.pms.sdk.core.facilitator.FacilitatorResponse.FacilitatorResponseBuilder;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorUtils;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.service.RestraintService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ResourceRestraintFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.RESOURCE_RESTRAINT).build();
  private static final String PLAN = "PLAN";

  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private RestraintService restraintService;
  @Inject private ResourceRestraintRegistry resourceRestraintRegistry;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private FacilitatorUtils facilitatorUtils;

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage) {
    Duration waitDuration = facilitatorUtils.extractWaitDurationFromDefaultParams(parameters);
    FacilitatorResponseBuilder responseBuilder = FacilitatorResponse.builder().initialWait(waitDuration);

    ResourceRestraintStepParameters stepParams = (ResourceRestraintStepParameters) stepParameters;
    final ResourceRestraint resourceRestraint = Preconditions.checkNotNull(
        restraintService.get(stepParams.getClaimantId(), stepParams.getResourceRestraintId()));
    final Constraint constraint = resourceRestraintService.createAbstraction(resourceRestraint);

    int permits = stepParams.getPermits();
    if (AcquireMode.ENSURE == stepParams.getAcquireMode()) {
      permits -= resourceRestraintService.getAllCurrentlyAcquiredPermits(
          stepParams.getHoldingScope().getScope(), getReleaseEntityId(stepParams, ambiance.getPlanExecutionId()));
    }

    ConstraintUnit renderedResourceUnit =
        new ConstraintUnit(engineExpressionService.renderExpression(ambiance, stepParams.getResourceUnit()));

    if (permits <= 0) {
      return responseBuilder.executionMode(ExecutionMode.SYNC).build();
    }
    List<Consumer> consumers = resourceRestraintRegistry.loadConsumers(constraint.getId(), renderedResourceUnit);
    Consumer.State state = constraint.calculateConsumerState(consumers, permits, Constraint.getUsedPermits(consumers));

    if (ACTIVE == state) {
      return responseBuilder.executionMode(ExecutionMode.SYNC).build();
    }

    return responseBuilder.executionMode(ExecutionMode.ASYNC).build();
  }

  private String getReleaseEntityId(ResourceRestraintStepParameters stepParameters, String planExecutionId) {
    String releaseEntityId;
    if (PLAN.equals(stepParameters.getHoldingScope().getScope())) {
      releaseEntityId = ResourceRestraintService.getReleaseEntityId(planExecutionId);
    } else {
      releaseEntityId = ResourceRestraintService.getReleaseEntityId(
          planExecutionId, stepParameters.getHoldingScope().getNodeSetupId());
    }
    return releaseEntityId;
  }
}
