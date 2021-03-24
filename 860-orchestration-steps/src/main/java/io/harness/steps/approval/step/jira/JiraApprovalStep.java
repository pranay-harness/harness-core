package io.harness.steps.approval.step.jira;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import java.util.Map;

public class JiraApprovalStep implements AsyncExecutable<JiraApprovalStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.JIRA_APPROVAL).build();

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, JiraApprovalStepParameters stepParameters, StepInputPackage inputPackage) {
    return AsyncExecutableResponse.newBuilder().build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, JiraApprovalStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return null;
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, JiraApprovalStepParameters stepParameters, AsyncExecutableResponse executableResponse) {}

  @Override
  public Class<JiraApprovalStepParameters> getStepParametersClass() {
    return JiraApprovalStepParameters.class;
  }
}
