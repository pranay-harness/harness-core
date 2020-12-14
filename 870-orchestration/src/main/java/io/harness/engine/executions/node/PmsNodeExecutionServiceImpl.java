package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.TaskMode;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.pms.steps.StepType;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class PmsNodeExecutionServiceImpl implements PmsNodeExecutionService {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void queueNodeExecution(NodeExecutionProto nodeExecution) {
    nodeExecutionService.save(NodeExecutionMapper.fromNodeExecutionProto(nodeExecution));
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(nodeExecution.getAmbiance()).orchestrationEngine(engine).build());
  }

  @Override
  public String queueTask(String nodeExecutionId, TaskMode mode, Map<String, String> setupAbstractions, Task task) {
    TaskExecutor taskExecutor = taskExecutorMap.get(mode.name());
    String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(setupAbstractions, task));
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
    ProgressCallback progressCallback = EngineProgressCallback.builder().nodeExecutionId(nodeExecutionId).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, progressCallback, taskId);
    return taskId;
  }

  @Override
  public void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds) {
    if (EmptyPredicate.isNotEmpty(callbackIds)) {
      NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));
    }

    if (status == Status.UNRECOGNIZED) {
      nodeExecutionService.update(
          nodeExecutionId, ops -> ops.addToSet(NodeExecutionKeys.executableResponses, executableResponse));
    } else {
      nodeExecutionService.update(
          nodeExecutionId, ops -> ops.addToSet(NodeExecutionKeys.executableResponses, executableResponse));
    }
  }

  @Override
  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponse stepResponse) {
    engine.handleStepResponse(nodeExecutionId, stepResponse);
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
    return JsonOrchestrationUtils.asObject(stepParameters, step.getStepParametersClass());
  }
}
