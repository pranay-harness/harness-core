package io.harness.cdng.sam;

import io.harness.delegate.task.aws.AwsSamTaskNGResponse;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.supplier.ThrowingSupplier;

public class SamDeployStep extends TaskExecutableWithRollback<AwsSamTaskNGResponse> {

  public static final StepType STEP_TYPE =
          StepType.newBuilder().setType(ExecutionNodeType.AWS_SAM_DEPLOY.getYamlType()).build();

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTask(Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    SamDeployStepInfo stepParametersSpec = (SamDeployStepInfo) stepParameters.getSpec();
    return null;
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepParameters, ThrowingSupplier<AwsSamTaskNGResponse> responseDataSupplier) throws Exception {
    return null;
  }
}
