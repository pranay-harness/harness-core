package io.harness.cdng.orchestration;

import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;

import java.util.Map;

public interface StepUtils {
  static StepResponse createStepResponseFromChildResponse(Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder();
    StepResponseNotifyData statusNotifyResponseData =
        (StepResponseNotifyData) responseDataMap.values().iterator().next();
    responseBuilder.status(statusNotifyResponseData.getStatus());
    return responseBuilder.build();
  }
}
