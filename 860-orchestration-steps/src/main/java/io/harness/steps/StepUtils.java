package io.harness.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.LogHelper.COMMAND_UNIT_PLACEHOLDER;

import static java.util.stream.Collectors.toList;

import io.harness.data.algorithm.HashGenerator;
import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.*;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.SimpleHDelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import software.wings.beans.LogHelper;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.util.*;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

public class StepUtils {
  private StepUtils() {}

  public static StepResponse createStepResponseFromChildResponse(Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().status(Status.SUCCEEDED);
    for (ResponseData responseData : responseDataMap.values()) {
      Status executionStatus = ((StepResponseNotifyData) responseData).getStatus();
      if (!StatusUtils.positiveStatuses().contains(executionStatus)) {
        responseBuilder.status(executionStatus);
      }
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
    logAbstractions.put("accountId", ambiance.getSetupAbstractionsMap().getOrDefault("accountId", ""));
    logAbstractions.put("orgId", ambiance.getSetupAbstractionsMap().getOrDefault("orgIdentifier", ""));
    logAbstractions.put("projectId", ambiance.getSetupAbstractionsMap().getOrDefault("projectIdentifier", ""));
    logAbstractions.put("pipelineExecutionId", ambiance.getPlanExecutionId());
    ambiance.getLevelsList()
        .stream()
        .filter(level -> level.getGroup().equals("stage"))
        .findFirst()
        .ifPresent(stageLevel -> logAbstractions.put("stageId", stageLevel.getIdentifier()));
    Level currentLevel = AmbianceUtils.obtainCurrentLevel(ambiance);
    if (currentLevel != null) {
      logAbstractions.put("stepRuntimeId", currentLevel.getRuntimeId());
    }
    return logAbstractions;
  }

  public static TaskRequest prepareTaskRequest(Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer) {
    return prepareTaskRequest(ambiance, taskData, kryoSerializer, new LinkedHashMap<>(), TaskCategory.DELEGATE_TASK_V2,
        Collections.emptyList());
  }

  public static TaskRequest prepareTaskRequest(Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer,
      LinkedHashMap<String, String> logAbstractions, TaskCategory taskCategory, List<String> units) {
    String accountId = Preconditions.checkNotNull(ambiance.getSetupAbstractionsMap().get("accountId"));
    TaskParameters taskParameters = (TaskParameters) taskData.getParameters()[0];
    List<ExecutionCapability> capabilities = new ArrayList<>();
    if (taskParameters instanceof ExecutionCapabilityDemander) {
      capabilities = ListUtils.emptyIfNull(
          ((ExecutionCapabilityDemander) taskParameters).fetchRequiredExecutionCapabilities(null));
    }
    LinkedHashMap<String, String> logAbstractionMap = generateLogAbstractions(ambiance);
    logAbstractionMap.putAll(MapUtils.emptyIfNull(logAbstractions));

    DelegateTaskRequest.Builder requestBuilder =
        DelegateTaskRequest.newBuilder()
            .setAccountId(accountId)
            .setDetails(
                TaskDetails.newBuilder()
                    .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(taskParameters) == null
                            ? new byte[] {}
                            : kryoSerializer.asDeflatedBytes(taskParameters)))
                    .setExecutionTimeout(Duration.newBuilder().setSeconds(taskData.getTimeout() * 1000).build())
                    // TODO : Change this spmehow and obtain from ambiance
                    .setExpressionFunctorToken(
                        Long.parseLong(ambiance.getSetupAbstractionsMap().get("expressionFunctorToken")))
                    .setMode(taskData.isAsync() ? TaskMode.ASYNC : TaskMode.SYNC)
                    .setParked(taskData.isParked())
                    .setType(TaskType.newBuilder().setType(taskData.getTaskType()).build())
                    .build())
            .addAllUnits(CollectionUtils.emptyIfNull(units))
            .addAllLogKeys(CollectionUtils.emptyIfNull(generateLogKeys(logAbstractionMap, units)))
            .setLogAbstractions(TaskLogAbstractions.newBuilder().putAllValues(logAbstractionMap).build())
            .setSetupAbstractions(TaskSetupAbstractions.newBuilder()
                                      .putAllValues((MapUtils.emptyIfNull(ambiance.getSetupAbstractionsMap())))
                                      .build());

    if (isNotEmpty(capabilities)) {
      requestBuilder.addAllCapabilities(
          capabilities.stream()
              .map(capability
                  -> Capability.newBuilder()
                         .setKryoCapability(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(capability) == null
                                 ? new byte[] {}
                                 : kryoSerializer.asDeflatedBytes(capability)))
                         .build())
              .collect(toList()));
    }

    return TaskRequest.newBuilder()
        .setDelegateTaskRequest(requestBuilder.build())
        .setTaskCategory(taskCategory)
        .build();
  }

  private static List<String> generateLogKeys(LinkedHashMap<String, String> logAbstractionMap, List<String> units) {
    List<String> unitKeys = new ArrayList<>();
    String baseLogKey = LogHelper.generateLogBaseKey(logAbstractionMap);
    for (String unit : units) {
      String logKey = baseLogKey + (isEmpty(baseLogKey) ? "" : String.format(COMMAND_UNIT_PLACEHOLDER, unit));
      unitKeys.add(logKey);
    }
    return unitKeys;
  }
}
