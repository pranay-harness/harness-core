package io.harness.cdng.orchestration;

import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.SimpleHDelegateTask;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import java.util.Map;

public class StepUtils {
  private StepUtils() {}

  public static StepResponse createStepResponseFromChildResponse(Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder();
    StepResponseNotifyData statusNotifyResponseData =
        (StepResponseNotifyData) responseDataMap.values().iterator().next();
    responseBuilder.status(statusNotifyResponseData.getStatus());
    return responseBuilder.build();
  }

  public static Task prepareDelegateTaskInput(
      String accountId, TaskData taskData, Map<String, String> setupAbstractions) {
    return createHDelegateTask(accountId, taskData, setupAbstractions);
  }

  private static Task createHDelegateTask(String accountId, TaskData taskData, Map<String, String> setupAbstractions) {
    return SimpleHDelegateTask.builder()
        .accountId(accountId)
        .data(taskData)
        .setupAbstractions(setupAbstractions)
        .build();
  }
}
