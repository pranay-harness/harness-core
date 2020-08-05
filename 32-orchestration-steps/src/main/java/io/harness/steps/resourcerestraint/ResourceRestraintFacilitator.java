package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.facilitator.FacilitatorResponse.FacilitatorResponseBuilder;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.service.RestraintService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@OwnedBy(CDC)
@Slf4j
public class ResourceRestraintFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.builder().type(FacilitatorType.RESOURCE_RESTRAINT).build();
  private static final String PLAN = "PLAN";

  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private RestraintService<? extends ResourceRestraint> restraintService;
  @Inject private ResourceRestraintRegistry resourceRestraintRegistry;

  @Override
  public FacilitatorResponse facilitate(Ambiance ambiance, StepParameters stepParameters,
      FacilitatorParameters parameters, StepInputPackage inputPackage) {
    FacilitatorResponseBuilder responseBuilder =
        FacilitatorResponse.builder().initialWait(parameters.getWaitDurationSeconds());

    ResourceRestraintStepParameters stepParams = (ResourceRestraintStepParameters) stepParameters;
    final ResourceRestraint resourceRestraint = Preconditions.checkNotNull(
        restraintService.get(stepParams.getClaimantId(), stepParams.getResourceRestraintId()));
    final Constraint constraint = resourceRestraintService.createAbstraction(resourceRestraint);

    int permits = stepParams.getPermits();
    if (AcquireMode.ENSURE == stepParams.getAcquireMode()) {
      permits -= resourceRestraintService.getAllCurrentlyAcquiredPermits(
          stepParams.getHoldingScope().getScope(), getReleaseEntityId(stepParams, ambiance.getPlanExecutionId()));
    }

    // TODO render restraint unit
    ConstraintUnit renderedResourceUnit = new ConstraintUnit(stepParams.getResourceUnit());

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
