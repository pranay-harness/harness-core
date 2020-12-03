package io.harness.cdng.k8s;

import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.common.NGTaskType;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.sm.states.k8s.K8sRollingDeployRollback;

import com.google.inject.Inject;
import java.util.Map;

public class K8sRollingRollbackStep implements TaskExecutable<K8sRollingRollbackStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.K8S_ROLLBACK_ROLLING.getName()).build();

  @Inject K8sStepHelper k8sStepHelper;
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public Class<K8sRollingRollbackStepParameters> getStepParametersClass() {
    return K8sRollingRollbackStepParameters.class;
  }

  @Override
  public Task obtainTask(
      Ambiance ambiance, K8sRollingRollbackStepParameters stepParameters, StepInputPackage inputPackage) {
    StepDependencySpec k8sRollingSpec =
        stepParameters.getStepDependencySpecs().get(CDStepDependencyKey.K8S_ROLL_OUT.name());
    K8sRollingOutcome k8sRollingOutcome = CDStepDependencyUtils.getK8sRolling(
        stepDependencyService, k8sRollingSpec, inputPackage, stepParameters, ambiance);

    StepDependencySpec infraSpec =
        stepParameters.getStepDependencySpecs().get(CDStepDependencyKey.INFRASTRUCTURE.name());
    InfrastructureOutcome infrastructure = CDStepDependencyUtils.getInfrastructure(
        stepDependencyService, infraSpec, inputPackage, stepParameters, ambiance);

    K8sRollingDeployRollbackTaskParameters taskParameters =
        K8sRollingDeployRollbackTaskParameters.builder()
            .activityId(UUIDGenerator.generateUuid())
            .releaseName(k8sRollingOutcome.getReleaseName())
            .releaseNumber(k8sRollingOutcome.getReleaseNumber())
            .commandName(K8sRollingDeployRollback.K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.DEPLOYMENT_ROLLING_ROLLBACK)
            .timeoutIntervalInMin(10 /*stepParameters.getTimeout().getValue()*/)
            .k8sClusterConfig(k8sStepHelper.getK8sClusterConfig(infrastructure, ambiance))
            .accountId(AmbianceHelper.getAccountId(ambiance))
            .build();

    return StepUtils.prepareDelegateTaskInput(AmbianceHelper.getAccountId(ambiance),
        TaskData.builder()
            .async(true)
            .timeout(600000 /*stepParameters.getTimeout().getValue()*/)
            .taskType(NGTaskType.K8S_COMMAND_TASK.name())
            .parameters(new Object[] {taskParameters})
            .build(),
        ambiance.getSetupAbstractionsMap());
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, K8sRollingRollbackStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) responseDataMap.values().iterator().next();

    if (executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } else {
      return StepResponse.builder().status(Status.FAILED).build();
    }
  }
}
