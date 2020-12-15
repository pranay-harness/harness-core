package io.harness.steps;

import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.SimpleHDelegateTask;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class StepUtils {
  private StepUtils() {}

  public static StepResponse createStepResponseFromChildResponse(Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder();
    StepResponseNotifyData statusNotifyResponseData =
        (StepResponseNotifyData) responseDataMap.values().iterator().next();
    // If suspended, then the final execution should be Success
    if (statusNotifyResponseData.getStatus() == Status.SUSPENDED) {
      responseBuilder.status(Status.SUCCEEDED);
    } else {
      responseBuilder.status(statusNotifyResponseData.getStatus());
    }
    return responseBuilder.build();
  }

  public static Task prepareDelegateTaskInput(String accountId, TaskData taskData,
      Map<String, String> setupAbstractions, LinkedHashMap<String, String> logAbstractions) {
    return createHDelegateTask(accountId, taskData, setupAbstractions, logAbstractions);
  }

  public static Task prepareDelegateTaskInput(
      String accountId, TaskData taskData, Map<String, String> setupAbstractions) {
    return createHDelegateTask(accountId, taskData, setupAbstractions, new LinkedHashMap<>());
  }

  private static Task createHDelegateTask(String accountId, TaskData taskData, Map<String, String> setupAbstractions,
      LinkedHashMap<String, String> logAbstractions) {
    return SimpleHDelegateTask.builder()
        .accountId(accountId)
        .data(taskData)
        .setupAbstractions(setupAbstractions)
        .logStreamingAbstractions(logAbstractions)
        .build();
  }

  @Nonnull
  public static LinkedHashMap<String, String> generateLogAbstractions(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = new LinkedHashMap<>();
    logAbstractions.put("pipelineExecutionId", ambiance.getPlanExecutionId());
    Level stageLevel = ambiance.getLevels(2);
    logAbstractions.put("stageIdentifier", stageLevel.getIdentifier());
    Level currentStepLevel = ambiance.getLevels(ambiance.getLevelsCount() - 1);
    logAbstractions.put("stepId", currentStepLevel.getRuntimeId());

    return logAbstractions;
  }
}
