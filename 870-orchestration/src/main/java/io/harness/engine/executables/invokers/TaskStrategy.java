package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.execution.Status.TASK_WAITING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executables.TaskExecuteStrategy;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.TaskExecutableResponse;
import io.harness.pms.execution.TaskMode;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.tasks.Task;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import lombok.Builder;
import lombok.NonNull;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class TaskStrategy implements TaskExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;

  private final TaskMode mode;

  @Builder
  public TaskStrategy(TaskMode mode) {
    this.mode = mode;
  }

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    TaskExecutable taskExecutable = extractTaskExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    Task task = taskExecutable.obtainTask(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, nodeExecution, task);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskExecutable taskExecutable = extractTaskExecutable(nodeExecution);
    StepResponse stepResponse = taskExecutable.handleTaskResult(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    pmsNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private TaskExecutable extractTaskExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (TaskExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(@NonNull Ambiance ambiance, NodeExecutionProto nodeExecution, Task task) {
    String taskId = Preconditions.checkNotNull(
        pmsNodeExecutionService.queueTask(nodeExecution.getUuid(), mode, ambiance.getSetupAbstractionsMap(), task));

    // Update Execution Node Instance state to TASK_WAITING
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), TASK_WAITING,
        ExecutableResponse.newBuilder()
            .setTask(TaskExecutableResponse.newBuilder().setTaskId(taskId).setTaskMode(mode).build())
            .build(),
        Collections.emptyList());
  }

  @Override
  public TaskMode getMode() {
    return mode;
  }
}
