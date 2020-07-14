package io.harness.cdng.k8s;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.common.AmbianceHelper;
import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.orchestration.StepUtils;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.execution.status.Status;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.facilitator.modes.taskv2.TaskV2Executable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.tasks.Task;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.sm.states.k8s.K8sRollingDeployRollback;

import java.util.Map;

public class K8sRollingRollbackStep implements Step, TaskExecutable, TaskV2Executable {
  public static final StepType STEP_TYPE = StepType.builder().type("K8S_ROLLBACK_ROLLING").build();

  @Inject K8sStepHelper k8sStepHelper;
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public Task obtainTask(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage) {
    K8sRollingRollbackStepParameters k8sRollingRollbackStepParameters =
        (K8sRollingRollbackStepParameters) stepParameters;

    StepDependencySpec k8sRollingSpec =
        k8sRollingRollbackStepParameters.getStepDependencySpecs().get(CDStepDependencyKey.K8S_ROLL_OUT.name());
    K8sRollingOutcome k8sRollingOutcome = CDStepDependencyUtils.getK8sRolling(
        stepDependencyService, k8sRollingSpec, inputPackage, stepParameters, ambiance);

    StepDependencySpec infraSpec =
        k8sRollingRollbackStepParameters.getStepDependencySpecs().get(CDStepDependencyKey.INFRASTRUCTURE.name());
    Infrastructure infrastructure = CDStepDependencyUtils.getInfrastructure(
        stepDependencyService, infraSpec, inputPackage, stepParameters, ambiance);

    K8sRollingDeployRollbackTaskParameters taskParameters =
        K8sRollingDeployRollbackTaskParameters.builder()
            .activityId(UUIDGenerator.generateUuid())
            .releaseName(k8sRollingOutcome.getReleaseName())
            .releaseNumber(k8sRollingOutcome.getReleaseNumber())
            .commandName(K8sRollingDeployRollback.K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.DEPLOYMENT_ROLLING_ROLLBACK)
            .timeoutIntervalInMin(k8sRollingRollbackStepParameters.getTimeout())
            .k8sClusterConfig(k8sStepHelper.getK8sClusterConfig(infrastructure, ambiance))
            .accountId(AmbianceHelper.getAccountId(ambiance))
            .build();

    return StepUtils.prepareDelegateTaskInput(AmbianceHelper.getAccountId(ambiance),
        TaskData.builder()
            .async(true)
            .timeout(k8sRollingRollbackStepParameters.getTimeout())
            .taskType(TaskType.K8S_COMMAND_TASK.name())
            .parameters(new Object[] {taskParameters})
            .build(),
        ambiance.getSetupAbstractions());
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) responseDataMap.values().iterator().next();

    if (executionResponse.getCommandExecutionStatus() == CommandExecutionResult.CommandExecutionStatus.SUCCESS) {
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } else {
      return StepResponse.builder().status(Status.FAILED).build();
    }
  }
}
