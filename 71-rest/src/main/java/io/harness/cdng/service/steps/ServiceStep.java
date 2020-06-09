package io.harness.cdng.service.steps;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.FailureType;
import io.harness.execution.status.Status;
import io.harness.executionplan.plancreator.beans.StepGroup;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.ChildrenExecutableResponseBuilder;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Slf4j
public class ServiceStep implements Step, ChildrenExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("SERVICE_STEP").build();

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    ServiceStepParameters parameters = (ServiceStepParameters) stepParameters;
    logger.info("Executing deployment stage with params [{}]", parameters);
    // TODO(archit): save service entity.

    ChildrenExecutableResponseBuilder responseBuilder = ChildrenExecutableResponse.builder();
    for (String nodeId : parameters.getParallelNodeIds()) {
      responseBuilder.child(ChildrenExecutableResponse.Child.builder().childNodeId(nodeId).build());
    }
    return responseBuilder.build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().status(Status.SUCCEEDED);
    ServiceStepParameters parameters = (ServiceStepParameters) stepParameters;
    boolean allChildrenSuccess = true;
    EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
    List<String> errorMessages = new ArrayList<>();
    for (ResponseData responseData : responseDataMap.values()) {
      StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) responseData;
      Status executionStatus = responseNotifyData.getStatus();
      if (executionStatus != Status.SUCCEEDED) {
        allChildrenSuccess = false;
        responseBuilder.status(executionStatus);
        errorMessages.add(responseNotifyData.getFailureInfo().getErrorMessage());
        failureTypes.addAll(responseNotifyData.getFailureInfo().getFailureTypes());
      }
    }
    if (!allChildrenSuccess) {
      responseBuilder.failureInfo(
          FailureInfo.builder().errorMessage(String.join(",", errorMessages)).failureTypes(failureTypes).build());
    } else {
      responseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                      .name("service")
                                      .outcome(parameters.getService())
                                      .group(StepGroup.STAGE.name())
                                      .build());
    }
    return responseBuilder.build();
  }
}
