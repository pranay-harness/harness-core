package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.InvocationHelper;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.FailureResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.ProgressCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class PmsNodeExecutionServiceImpl implements PmsNodeExecutionService {
  @Inject private Map<TaskCategory, TaskExecutor> taskExecutorMap;
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private InvocationHelper invocationHelper;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private ResponseDataMapper responseDataMapper;

  @Override
  public void queueNodeExecution(NodeExecutionProto nodeExecution) {
    nodeExecutionService.save(NodeExecutionMapper.fromNodeExecutionProto(nodeExecution));
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(nodeExecution.getAmbiance()).orchestrationEngine(engine).build());
  }

  @Override
  public String queueTask(String nodeExecutionId, Map<String, String> setupAbstractions, TaskRequest taskRequest) {
    TaskExecutor taskExecutor = taskExecutorMap.get(taskRequest.getTaskCategory());
    String taskId =
        Preconditions.checkNotNull(taskExecutor.queueTask(setupAbstractions, taskRequest, Duration.ofSeconds(0)));
    OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
    ProgressCallback progressCallback = EngineProgressCallback.builder().nodeExecutionId(nodeExecutionId).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, progressCallback, taskId);
    return taskId;
  }

  @Override
  public void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds) {
    if (EmptyPredicate.isNotEmpty(callbackIds)) {
      OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));
    }

    if (status == Status.NO_OP) {
      nodeExecutionService.update(
          nodeExecutionId, ops -> ops.addToSet(NodeExecutionKeys.executableResponses, executableResponse));
    } else {
      nodeExecutionService.updateStatusWithOps(
          nodeExecutionId, status, ops -> ops.addToSet(NodeExecutionKeys.executableResponses, executableResponse));
    }
  }

  @Override
  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    engine.handleStepResponse(nodeExecutionId, stepResponse);
  }

  @Override
  public void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);
    engine.resume(nodeExecutionId, responseBytes, asyncError);
  }

  @Override
  public Map<String, ResponseData> accumulateResponses(String planExecutionId, String notifyId) {
    return invocationHelper.accumulateResponses(planExecutionId, notifyId);
  }

  @Override
  public StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getResolvedStepParameters());
  }

  private StepParameters extractStepParametersInternal(StepType stepType, String stepParameters) {
    Step<?> step = stepRegistry.obtain(stepType);
    if (isEmpty(stepParameters)) {
      return null;
    }
    return RecastOrchestrationUtils.fromDocumentJson(stepParameters, step.getStepParametersClass());
  }

  @Override
  public void handleAdviserResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse) {
    waitNotifyEngine.doneWith(notifyId, BinaryResponseData.builder().data(adviserResponse.toByteArray()).build());
  }

  @Override
  public void handleFacilitationResponse(
      @NonNull String nodeExecutionId, String notifyId, FacilitatorResponseProto facilitatorResponseProto) {
    waitNotifyEngine.doneWith(
        notifyId, BinaryResponseData.builder().data(facilitatorResponseProto.toByteArray()).build());
  }

  @Override
  public void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo) {
    waitNotifyEngine.doneWith(eventNotifyId,
        FailureResponseData.builder()
            .errorMessage(failureInfo.getErrorMessage())
            .failureTypes(EngineExceptionUtils.transformToWingsFailureTypes(failureInfo.getFailureTypesList()))
            .build());
  }
}
