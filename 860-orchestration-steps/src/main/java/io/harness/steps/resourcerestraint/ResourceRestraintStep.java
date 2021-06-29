package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.shared.ResourceRestraint;
import io.harness.beans.shared.RestraintService;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableMode;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintOutcome;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.utils.ResourceRestraintUtils;
import io.harness.tasks.ResponseData;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class ResourceRestraintStep
    implements SyncExecutable<StepElementParameters>, AsyncExecutable<StepElementParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(StepSpecTypeConstants.RESOURCE_CONSTRAINT).build();

  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private RestraintService restraintService;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepElementParameters stepElementParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    ResourceRestraintSpecParameters specParameters = (ResourceRestraintSpecParameters) stepElementParameters.getSpec();
    final ResourceRestraint resourceRestraint =
        restraintService.getByNameAndAccountId(specParameters.getName(), AmbianceUtils.getAccountId(ambiance));
    String releaseEntityId = ResourceRestraintUtils.getReleaseEntityId(specParameters, ambiance.getPlanExecutionId());

    return StepResponse.builder()
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(STEP_TYPE.getType())
                         .outcome(ResourceRestraintOutcome.builder()
                                      .name(resourceRestraint.getName())
                                      .capacity(resourceRestraint.getCapacity())
                                      .resourceUnit(specParameters.getResourceUnit())
                                      .usage(specParameters.getPermits())
                                      .alreadyAcquiredPermits(getAlreadyAcquiredPermits(
                                          specParameters.getHoldingScope().getScope(), releaseEntityId))
                                      .build())
                         .build())
        .status(Status.SUCCEEDED)
        .build();
  }

  @Override
  public AsyncExecutableResponse executeAsync(Ambiance ambiance, StepElementParameters stepElementParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    ResourceRestraintPassThroughData restraintPassThroughData = (ResourceRestraintPassThroughData) passThroughData;

    return AsyncExecutableResponse.newBuilder()
        .addCallbackIds(restraintPassThroughData.getConsumerId())
        .setMode(AsyncExecutableMode.RESOURCE_WAITING_MODE)
        .build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, StepElementParameters stepElementParameters, Map<String, ResponseData> responseDataMap) {
    ResourceRestraintSpecParameters specParameters = (ResourceRestraintSpecParameters) stepElementParameters.getSpec();

    final ResourceRestraint resourceRestraint =
        restraintService.getByNameAndAccountId(specParameters.getName(), AmbianceUtils.getAccountId(ambiance));

    resourceRestraintService.updateBlockedConstraints(ImmutableSet.of(resourceRestraint.getUuid()));

    return StepResponse.builder()
        .stepOutcome(
            StepResponse.StepOutcome.builder()
                .name(STEP_TYPE.getType())
                .outcome(
                    ResourceRestraintOutcome.builder()
                        .name(resourceRestraint.getName())
                        .capacity(resourceRestraint.getCapacity())
                        .resourceUnit(specParameters.getResourceUnit())
                        .usage(specParameters.getPermits())
                        .alreadyAcquiredPermits(getAlreadyAcquiredPermits(specParameters.getHoldingScope().getScope(),
                            ResourceRestraintUtils.getReleaseEntityId(specParameters, ambiance.getPlanExecutionId())))
                        .build())
                .build())
        .status(Status.SUCCEEDED)
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, StepElementParameters stepElementParameters, AsyncExecutableResponse executableResponse) {
    ResourceRestraintSpecParameters specParameters = (ResourceRestraintSpecParameters) stepElementParameters.getSpec();

    resourceRestraintService.finishInstance(
        Preconditions.checkNotNull(executableResponse.getCallbackIdsList().get(0),
            "CallbackId should not be null in handleAbort() for nodeExecution with id %s",
            AmbianceUtils.obtainCurrentRuntimeId(ambiance)),
        specParameters.getResourceUnit());
  }

  private int getAlreadyAcquiredPermits(String holdingScope, String releaseEntityId) {
    return resourceRestraintService.getAllCurrentlyAcquiredPermits(holdingScope, releaseEntityId);
  }
}
