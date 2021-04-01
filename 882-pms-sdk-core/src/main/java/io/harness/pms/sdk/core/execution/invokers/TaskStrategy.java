package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.Status.NO_OP;
import static io.harness.pms.contracts.execution.Status.SKIPPED;
import static io.harness.pms.contracts.execution.Status.TASK_WAITING;
import static io.harness.pms.sdk.core.execution.invokers.StrategyHelper.buildResponseDataSupplier;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.SkipTaskExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest.RequestCase;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.data.StringOutcome;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class TaskStrategy implements ExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    TaskExecutable taskExecutable = extractTaskExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskRequest task = taskExecutable.obtainTask(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, nodeExecution, task);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskExecutable taskExecutable = extractTaskExecutable(nodeExecution);
    StepResponse stepResponse =
        taskExecutable.handleTaskResult(ambiance, pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution),
            buildResponseDataSupplier(resumePackage.getResponseDataMap()));
    pmsNodeExecutionService.handleStepResponse(
        nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  private TaskExecutable extractTaskExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (TaskExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(@NonNull Ambiance ambiance, NodeExecutionProto nodeExecution, TaskRequest taskRequest) {
    if (RequestCase.SKIPTASKREQUEST == taskRequest.getRequestCase()) {
      pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), NO_OP,
          ExecutableResponse.newBuilder()
              .setSkipTask(SkipTaskExecutableResponse.newBuilder()
                               .setMessage(taskRequest.getSkipTaskRequest().getMessage())
                               .build())
              .build(),
          Collections.emptyList());
      pmsNodeExecutionService.handleStepResponse(nodeExecution.getUuid(),
          StepResponseMapper.toStepResponseProto(
              StepResponse.builder()
                  .status(SKIPPED)
                  .stepOutcome(
                      StepOutcome.builder()
                          .name("skipOutcome")
                          .outcome(
                              StringOutcome.builder().message(taskRequest.getSkipTaskRequest().getMessage()).build())
                          .build())
                  .build()));
      return;
    }

    String taskId = Preconditions.checkNotNull(
        pmsNodeExecutionService.queueTask(nodeExecution.getUuid(), ambiance.getSetupAbstractionsMap(), taskRequest));

    // Update Execution Node Instance state to TASK_WAITING
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), TASK_WAITING,
        ExecutableResponse.newBuilder()
            .setTask(
                TaskExecutableResponse.newBuilder()
                    .setTaskId(taskId)
                    .setTaskCategory(taskRequest.getTaskCategory())
                    .addAllLogKeys(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getLogKeysList()))
                    .addAllUnits(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getUnitsList()))
                    .setTaskName(taskRequest.getDelegateTaskRequest().getTaskName())
                    .build())
            .build(),
        Collections.emptyList());
  }
}
