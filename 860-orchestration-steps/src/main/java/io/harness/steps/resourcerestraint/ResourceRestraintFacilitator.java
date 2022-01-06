/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.pms.sdk.core.execution.events.node.facilitate.FacilitatorResponse.FacilitatorResponseBuilder;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.distribution.constraint.InvalidPermitsException;
import io.harness.distribution.constraint.PermanentlyBlockedConsumerException;
import io.harness.distribution.constraint.UnableToRegisterConsumerException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.facilitator.FacilitatorUtils;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.execution.events.node.facilitate.Facilitator;
import io.harness.pms.sdk.core.execution.events.node.facilitate.FacilitatorResponse;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance.ResourceRestraintInstanceKeys;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceService;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.utils.ResourceRestraintUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class ResourceRestraintFacilitator implements Facilitator {
  public static final String RESOURCE_RESTRAINT = "RESOURCE_RESTRAINT";

  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(RESOURCE_RESTRAINT).build();

  @Inject private ResourceRestraintInstanceService resourceRestraintInstanceService;
  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private ResourceRestraintRegistry resourceRestraintRegistry;
  @Inject private PmsEngineExpressionService pmsEngineExpressionService;
  @Inject private FacilitatorUtils facilitatorUtils;

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage) {
    StepElementParameters stepElementParameters = (StepElementParameters) stepParameters;
    FacilitatorResponseBuilder responseBuilder = FacilitatorResponse.builder();

    ResourceRestraintSpecParameters specParameters = (ResourceRestraintSpecParameters) stepElementParameters.getSpec();
    final ResourceRestraint resourceRestraint = Preconditions.checkNotNull(
        resourceRestraintService.getByNameAndAccountId(specParameters.getName(), AmbianceUtils.getAccountId(ambiance)));
    final Constraint constraint = resourceRestraintInstanceService.createAbstraction(resourceRestraint);
    String releaseEntityId = ResourceRestraintUtils.getReleaseEntityId(specParameters, ambiance.getPlanExecutionId());

    int permits = specParameters.getPermits();
    if (AcquireMode.ENSURE == specParameters.getAcquireMode()) {
      permits -= resourceRestraintInstanceService.getAllCurrentlyAcquiredPermits(
          specParameters.getHoldingScope().getScope(), releaseEntityId);
    }

    ConstraintUnit renderedResourceUnit =
        new ConstraintUnit(pmsEngineExpressionService.renderExpression(ambiance, specParameters.getResourceUnit()));

    if (permits <= 0) {
      return responseBuilder.executionMode(ExecutionMode.SYNC).build();
    }

    Map<String, Object> constraintContext =
        populateConstraintContext(resourceRestraint, specParameters, releaseEntityId);

    String consumerId = generateUuid();
    try {
      Consumer.State state = constraint.registerConsumer(
          renderedResourceUnit, new ConsumerId(consumerId), permits, constraintContext, resourceRestraintRegistry);

      if (ACTIVE == state) {
        return responseBuilder.executionMode(ExecutionMode.SYNC).build();
      }
    } catch (InvalidPermitsException | UnableToRegisterConsumerException | PermanentlyBlockedConsumerException e) {
      log.error("Exception on ResourceRestraintStep for id [{}]", AmbianceUtils.obtainCurrentRuntimeId(ambiance), e);
    }

    return responseBuilder.executionMode(ExecutionMode.CONSTRAINT)
        .passThroughData(ResourceRestraintPassThroughData.builder().consumerId(consumerId).build())
        .build();
  }

  private Map<String, Object> populateConstraintContext(
      ResourceRestraint resourceRestraint, ResourceRestraintSpecParameters stepParameters, String releaseEntityId) {
    Map<String, Object> constraintContext = new HashMap<>();
    constraintContext.put(ResourceRestraintInstanceKeys.releaseEntityType, stepParameters.getHoldingScope().getScope());
    constraintContext.put(ResourceRestraintInstanceKeys.releaseEntityId, releaseEntityId);
    constraintContext.put(ResourceRestraintInstanceKeys.order,
        resourceRestraintInstanceService.getMaxOrder(resourceRestraint.getUuid()) + 1);

    return constraintContext;
  }
}
