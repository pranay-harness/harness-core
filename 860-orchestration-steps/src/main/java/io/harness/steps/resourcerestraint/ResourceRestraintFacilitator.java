package io.harness.steps.resourcerestraint;

import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.pms.sdk.core.facilitator.FacilitatorResponse.FacilitatorResponseBuilder;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.shared.ResourceRestraint;
import io.harness.beans.shared.RestraintService;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorUtils;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.utils.PmsConstants;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class ResourceRestraintFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.RESOURCE_RESTRAINT).build();

  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private RestraintService restraintService;
  @Inject private ResourceRestraintRegistry resourceRestraintRegistry;
  @Inject private PmsEngineExpressionService pmsEngineExpressionService;
  @Inject private FacilitatorUtils facilitatorUtils;

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage) {
    StepElementParameters stepElementParameters = (StepElementParameters) stepParameters;
    Duration waitDuration = facilitatorUtils.extractWaitDurationFromDefaultParams(parameters);
    FacilitatorResponseBuilder responseBuilder = FacilitatorResponse.builder().initialWait(waitDuration);

    ResourceRestraintSpecParameters specParameters = (ResourceRestraintSpecParameters) stepElementParameters.getSpec();
    final ResourceRestraint resourceRestraint = Preconditions.checkNotNull(
        restraintService.getByNameAndAccountId(specParameters.getName(), AmbianceUtils.getAccountId(ambiance)));
    final Constraint constraint = resourceRestraintService.createAbstraction(resourceRestraint);

    int permits = specParameters.getPermits();
    if (AcquireMode.ENSURE == specParameters.getAcquireMode()) {
      permits -= resourceRestraintService.getAllCurrentlyAcquiredPermits(specParameters.getHoldingScope().getScope(),
          getReleaseEntityId(specParameters, ambiance.getPlanExecutionId()));
    }

    ConstraintUnit renderedResourceUnit =
        new ConstraintUnit(pmsEngineExpressionService.renderExpression(ambiance, specParameters.getResourceUnit()));

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

  private String getReleaseEntityId(ResourceRestraintSpecParameters specParameters, String planExecutionId) {
    String releaseEntityId;
    if (PmsConstants.RELEASE_ENTITY_TYPE_PLAN.equals(specParameters.getHoldingScope().getScope())) {
      releaseEntityId = ResourceRestraintService.getReleaseEntityId(planExecutionId);
    } else {
      releaseEntityId = ResourceRestraintService.getReleaseEntityId(
          planExecutionId, specParameters.getHoldingScope().getNodeSetupId());
    }
    return releaseEntityId;
  }
}
