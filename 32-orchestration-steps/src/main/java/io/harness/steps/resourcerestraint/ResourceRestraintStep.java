package io.harness.steps.resourcerestraint;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class ResourceRestraintStep implements Step, AsyncExecutable<ResourceRestraintStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("RESOURCE_RESTRAINT").build();

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, ResourceRestraintStepParameters stepParameters, StepInputPackage inputPackage) {
    String consumerId = generateUuid();
    return AsyncExecutableResponse.builder().callbackId(consumerId).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, ResourceRestraintStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, ResourceRestraintStepParameters stateParameters, AsyncExecutableResponse executableResponse) {
    // TODO implement abort handling
    throw new UnsupportedOperationException();
  }
}
