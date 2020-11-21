package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.distribution.constraint.InvalidPermitsException;
import io.harness.distribution.constraint.PermanentlyBlockedConsumerException;
import io.harness.distribution.constraint.UnableToRegisterConsumerException;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance.ResourceRestraintInstanceKeys;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintOutcome;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.service.RestraintService;
import io.harness.tasks.ResponseData;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ResourceRestraintStep
    implements Step, SyncExecutable<ResourceRestraintStepParameters>, AsyncExecutable<ResourceRestraintStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.RESOURCE_RESTRAINT).build();
  private static final String PLAN = "PLAN";

  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private RestraintService restraintService;
  @Inject private ResourceRestraintRegistry resourceRestraintRegistry;
  @Inject private EngineExpressionService engineExpressionService;

  @Override
  public StepResponse executeSync(Ambiance ambiance, ResourceRestraintStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    final ResourceRestraint resourceRestraint =
        restraintService.get(stepParameters.getClaimantId(), stepParameters.getResourceRestraintId());
    String releaseEntityId = getReleaseEntityId(stepParameters, ambiance.getPlanExecutionId());

    executeInternal(resourceRestraint, stepParameters, ambiance, releaseEntityId, false);

    return StepResponse.builder()
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(STEP_TYPE.getType())
                         .outcome(ResourceRestraintOutcome.builder()
                                      .name(resourceRestraint.getName())
                                      .capacity(resourceRestraint.getCapacity())
                                      .resourceUnit(stepParameters.getResourceUnit())
                                      .usage(stepParameters.getPermits())
                                      .alreadyAcquiredPermits(getAlreadyAcquiredPermits(
                                          stepParameters.getHoldingScope().getScope(), releaseEntityId))
                                      .build())
                         .build())
        .status(Status.SUCCEEDED)
        .build();
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, ResourceRestraintStepParameters stepParameters, StepInputPackage inputPackage) {
    final ResourceRestraint resourceRestraint =
        restraintService.get(stepParameters.getClaimantId(), stepParameters.getResourceRestraintId());
    String releaseEntityId = getReleaseEntityId(stepParameters, ambiance.getPlanExecutionId());

    String consumerId = executeInternal(resourceRestraint, stepParameters, ambiance, releaseEntityId, true);

    return AsyncExecutableResponse.builder().callbackId(consumerId).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, ResourceRestraintStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    final ResourceRestraint resourceRestraint =
        restraintService.get(stepParameters.getClaimantId(), stepParameters.getResourceRestraintId());

    resourceRestraintService.updateBlockedConstraints(ImmutableSet.of(resourceRestraint.getUuid()));

    return StepResponse.builder()
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(STEP_TYPE.getType())
                         .outcome(ResourceRestraintOutcome.builder()
                                      .name(resourceRestraint.getName())
                                      .capacity(resourceRestraint.getCapacity())
                                      .resourceUnit(stepParameters.getResourceUnit())
                                      .usage(stepParameters.getPermits())
                                      .alreadyAcquiredPermits(
                                          getAlreadyAcquiredPermits(stepParameters.getHoldingScope().getScope(),
                                              getReleaseEntityId(stepParameters, ambiance.getPlanExecutionId())))
                                      .build())
                         .build())
        .status(Status.SUCCEEDED)
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, ResourceRestraintStepParameters stepParameters, AsyncExecutableResponse executableResponse) {
    resourceRestraintService.finishInstance(
        Preconditions.checkNotNull(executableResponse.getCallbackIds().get(0),
            "CallbackId should not be null in handleAbort() for nodeExecution with id %s",
            ambiance.obtainCurrentLevel().getRuntimeId()),
        stepParameters.getResourceUnit());
  }

  private String executeInternal(ResourceRestraint resourceRestraint, ResourceRestraintStepParameters stepParameters,
      Ambiance ambiance, String releaseEntityId, boolean isAsync) {
    final Constraint constraint = resourceRestraintService.createAbstraction(resourceRestraint);

    int permits = calculatePermits(stepParameters, ambiance);

    ConstraintUnit renderedResourceUnit =
        new ConstraintUnit(engineExpressionService.renderExpression(ambiance, stepParameters.getResourceUnit()));

    Map<String, Object> constraintContext = populateConstraintContext(stepParameters, releaseEntityId);

    String consumerId = generateUuid();
    try {
      Consumer.State state = constraint.registerConsumer(
          renderedResourceUnit, new ConsumerId(consumerId), permits, constraintContext, resourceRestraintRegistry);
      if (isAsync && ACTIVE == state) {
        throw new InvalidRequestException("The state should be BLOCKED for consumer with id [" + consumerId + "].");
      } else if (!isAsync && BLOCKED == state) {
        throw new InvalidRequestException("The state should be ACTIVE for consumer with id [" + consumerId + "].");
      }
    } catch (InvalidPermitsException | UnableToRegisterConsumerException | PermanentlyBlockedConsumerException e) {
      log.error("Exception on ResourceRestraintStep for id [{}]", ambiance.obtainCurrentRuntimeId(), e);
    }

    return consumerId;
  }

  private int getAlreadyAcquiredPermits(String holdingScope, String releaseEntityId) {
    return resourceRestraintService.getAllCurrentlyAcquiredPermits(holdingScope, releaseEntityId);
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

  private int calculatePermits(ResourceRestraintStepParameters stepParameters, Ambiance ambiance) {
    int permits = stepParameters.getPermits();
    if (AcquireMode.ENSURE == stepParameters.getAcquireMode()) {
      permits -= resourceRestraintService.getAllCurrentlyAcquiredPermits(
          stepParameters.getHoldingScope().getScope(), ambiance.getPlanExecutionId());
    }
    return permits;
  }

  private Map<String, Object> populateConstraintContext(
      ResourceRestraintStepParameters stepParameters, String releaseEntityId) {
    Map<String, Object> constraintContext = new HashMap<>();
    constraintContext.put(ResourceRestraintInstanceKeys.releaseEntityType, stepParameters.getHoldingScope().getScope());
    constraintContext.put(ResourceRestraintInstanceKeys.releaseEntityId, releaseEntityId);
    constraintContext.put(ResourceRestraintInstanceKeys.order,
        resourceRestraintService.getMaxOrder(stepParameters.getResourceRestraintId()) + 1);

    return constraintContext;
  }
}
