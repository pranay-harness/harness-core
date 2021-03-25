package io.harness.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.LogHelper.COMMAND_UNIT_PLACEHOLDER;

import static java.util.stream.Collectors.toList;

import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.Capability;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskLogAbstractions;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.SimpleHDelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.logstreaming.LogStreamingHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

public class StepUtils {
  private StepUtils() {}

  public static StepResponse createStepResponseFromChildResponse(Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().status(Status.SUCCEEDED);

    for (ResponseData responseData : responseDataMap.values()) {
      if (((StepResponseNotifyData) responseData).getStatus() == Status.FAILED) {
        responseBuilder.status(Status.FAILED);
        responseBuilder.failureInfo(((StepResponseNotifyData) responseData).getFailureInfo());
        return responseBuilder.build();
      }
    }

    for (ResponseData responseData : responseDataMap.values()) {
      StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) responseData;
      Status executionStatus = responseNotifyData.getStatus();
      if (!StatusUtils.positiveStatuses().contains(executionStatus)) {
        responseBuilder.status(executionStatus);
      }
      if (StatusUtils.brokeStatuses().contains(executionStatus)) {
        responseBuilder.failureInfo(responseNotifyData.getFailureInfo());
      }
    }
    return responseBuilder.build();
  }

  public static RollbackOutcome getFailedChildRollbackOutcome(
      Map<String, ResponseData> responseDataMap, OutcomeService outcomeService) {
    for (ResponseData responseData : responseDataMap.values()) {
      StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) responseData;
      Status executionStatus = responseNotifyData.getStatus();
      if (!StatusUtils.positiveStatuses().contains(executionStatus)
          || StatusUtils.brokeStatuses().contains(executionStatus)) {
        if (responseNotifyData.getStepOutcomeRefs() == null) {
          return null;
        }

        for (StepOutcomeRef stepOutcomeRef : responseNotifyData.getStepOutcomeRefs()) {
          Outcome outcome = outcomeService.fetchOutcome(stepOutcomeRef.getInstanceId());
          if (outcome instanceof RollbackOutcome) {
            RollbackOutcome rollbackOutcome = (RollbackOutcome) outcome;
            if (rollbackOutcome.getRollbackInfo() == null) {
              return null;
            }
            return rollbackOutcome;
          }
        }
      }
    }
    return null;
  }

  public static StepResponse createStepResponseFromChildWithChildOutcomes(
      Map<String, ResponseData> responseDataMap, OutcomeService outcomeService) {
    StepResponseBuilder responseBuilder = StepResponse.builder().status(Status.SUCCEEDED);
    for (ResponseData responseData : responseDataMap.values()) {
      StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) responseData;
      Status executionStatus = responseNotifyData.getStatus();
      if (!StatusUtils.positiveStatuses().contains(executionStatus)) {
        responseBuilder.status(executionStatus);
      }
      if (StatusUtils.brokeStatuses().contains(executionStatus)) {
        responseBuilder.failureInfo(responseNotifyData.getFailureInfo());
        for (StepOutcomeRef stepOutcomeRef : responseNotifyData.getStepOutcomeRefs()) {
          Outcome outcome = outcomeService.fetchOutcome(stepOutcomeRef.getInstanceId());
          responseBuilder.stepOutcome(StepOutcome.builder().name(stepOutcomeRef.getName()).outcome(outcome).build());
        }
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
  public static LinkedHashMap<String, String> generateStageLogAbstractions(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = new LinkedHashMap<>();
    logAbstractions.put("accountId", ambiance.getSetupAbstractionsMap().getOrDefault("accountId", ""));
    logAbstractions.put("orgId", ambiance.getSetupAbstractionsMap().getOrDefault("orgIdentifier", ""));
    logAbstractions.put("projectId", ambiance.getSetupAbstractionsMap().getOrDefault("projectIdentifier", ""));
    logAbstractions.put("pipelineExecutionId", ambiance.getPlanExecutionId());
    ambiance.getLevelsList()
        .stream()
        .filter(level -> level.getGroup().equals("STAGE"))
        .findFirst()
        .ifPresent(stageLevel -> logAbstractions.put("stageId", stageLevel.getIdentifier()));
    return logAbstractions;
  }

  @Nonnull
  public static LinkedHashMap<String, String> generateLogAbstractions(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = generateStageLogAbstractions(ambiance);
    Level currentLevel = AmbianceUtils.obtainCurrentLevel(ambiance);
    if (currentLevel != null) {
      logAbstractions.put("stepRuntimeId", currentLevel.getRuntimeId());
    }
    return logAbstractions;
  }

  public static TaskRequest prepareTaskRequest(Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer) {
    return prepareTaskRequest(
        ambiance, taskData, kryoSerializer, TaskCategory.DELEGATE_TASK_V2, Collections.emptyList(), true, null);
  }

  public static TaskRequest prepareTaskRequestWithTaskSelector(
      Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer, List<TaskSelector> selectors) {
    return prepareTaskRequest(ambiance, taskData, kryoSerializer, TaskCategory.DELEGATE_TASK_V2,
        Collections.emptyList(), true, null, selectors);
  }

  public static TaskRequest prepareTaskRequestWithoutLogs(
      Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer) {
    return prepareTaskRequest(
        ambiance, taskData, kryoSerializer, TaskCategory.DELEGATE_TASK_V2, Collections.emptyList(), false, null);
  }

  public static TaskRequest prepareTaskRequest(
      Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer, List<String> units) {
    return prepareTaskRequest(ambiance, taskData, kryoSerializer, TaskCategory.DELEGATE_TASK_V2, units, true, null);
  }

  public static TaskRequest prepareTaskRequest(Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer,
      TaskCategory taskCategory, List<String> units, boolean withLogs, String taskName) {
    return prepareTaskRequest(ambiance, taskData, kryoSerializer, taskCategory, units, withLogs, taskName, null);
  }

  public static TaskRequest prepareTaskRequest(Ambiance ambiance, TaskData taskData, KryoSerializer kryoSerializer,
      TaskCategory taskCategory, List<String> units, boolean withLogs, String taskName, List<TaskSelector> selectors) {
    String accountId = Preconditions.checkNotNull(ambiance.getSetupAbstractionsMap().get("accountId"));
    TaskParameters taskParameters = (TaskParameters) taskData.getParameters()[0];
    List<ExecutionCapability> capabilities = new ArrayList<>();
    if (taskParameters instanceof ExecutionCapabilityDemander) {
      capabilities = ListUtils.emptyIfNull(
          ((ExecutionCapabilityDemander) taskParameters).fetchRequiredExecutionCapabilities(null));
    }
    LinkedHashMap<String, String> logAbstractionMap =
        withLogs ? generateLogAbstractions(ambiance) : new LinkedHashMap<>();
    units = withLogs ? units : new ArrayList<>();

    DelegateTaskRequest.Builder requestBuilder =
        DelegateTaskRequest.newBuilder()
            .setAccountId(accountId)
            .setDetails(
                TaskDetails.newBuilder()
                    .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(taskParameters) == null
                            ? new byte[] {}
                            : kryoSerializer.asDeflatedBytes(taskParameters)))
                    .setExecutionTimeout(Duration.newBuilder().setSeconds(taskData.getTimeout() * 1000).build())
                    .setExpressionFunctorToken(ambiance.getExpressionFunctorToken())
                    .setMode(taskData.isAsync() ? TaskMode.ASYNC : TaskMode.SYNC)
                    .setParked(taskData.isParked())
                    .setType(TaskType.newBuilder().setType(taskData.getTaskType()).build())
                    .build())
            .addAllUnits(CollectionUtils.emptyIfNull(units))
            .addAllSelectors(CollectionUtils.emptyIfNull(selectors))
            .addAllLogKeys(CollectionUtils.emptyIfNull(generateLogKeys(logAbstractionMap, units)))
            .setLogAbstractions(TaskLogAbstractions.newBuilder().putAllValues(logAbstractionMap).build())
            .setSetupAbstractions(TaskSetupAbstractions.newBuilder()
                                      .putAllValues(MapUtils.emptyIfNull(ambiance.getSetupAbstractionsMap()))
                                      .build())
            .setSelectionTrackingLogEnabled(true)
            .setTaskName(taskName == null ? taskData.getTaskType() : taskName);

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

  public static List<String> generateLogKeys(Ambiance ambiance, List<String> units) {
    LinkedHashMap<String, String> logAbstractionMap = generateLogAbstractions(ambiance);
    return generateLogKeys(logAbstractionMap, units);
  }

  public static List<String> generateLogKeys(LinkedHashMap<String, String> logAbstractionMap, List<String> units) {
    if (isEmpty(logAbstractionMap)) {
      return Collections.emptyList();
    }
    String baseLogKey = LogStreamingHelper.generateLogBaseKey(logAbstractionMap);
    if (isEmpty(units)) {
      return Collections.singletonList(baseLogKey);
    }

    List<String> unitKeys = new ArrayList<>();
    for (String unit : units) {
      String logKey = baseLogKey + (isEmpty(baseLogKey) ? "" : String.format(COMMAND_UNIT_PLACEHOLDER, unit));
      unitKeys.add(logKey);
    }
    return unitKeys;
  }

  public static boolean isStepInRollbackSection(Ambiance ambiance) {
    List<Level> levelsList = ambiance.getLevelsList();
    if (isEmpty(levelsList)) {
      return false;
    }

    Optional<Level> optionalLevel =
        ambiance.getLevelsList()
            .stream()
            .filter(level -> level.getStepType().getType().equals("ROLLBACK_OPTIONAL_CHILD_CHAIN"))
            .findFirst();

    return optionalLevel.isPresent();
  }
}
