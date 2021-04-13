package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableMode;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.ApprovalNotificationHandler;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(CDC)
public class HarnessApprovalStep implements AsyncExecutable<HarnessApprovalStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(StepSpecTypeConstants.HARNESS_APPROVAL).build();

  @Inject private ApprovalInstanceService approvalInstanceService;
  @Inject private ApprovalNotificationHandler approvalNotificationHandler;

  @Override
  public Class<HarnessApprovalStepParameters> getStepParametersClass() {
    return HarnessApprovalStepParameters.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters, StepInputPackage inputPackage) {
    HarnessApprovalInstance approvalInstance = HarnessApprovalInstance.fromStepParameters(ambiance, stepParameters);
    approvalInstance = (HarnessApprovalInstance) approvalInstanceService.save(approvalInstance);
    approvalNotificationHandler.sendNotification(approvalInstance, ambiance);

    return AsyncExecutableResponse.newBuilder()
        .addCallbackIds(approvalInstance.getId())
        .setMode(AsyncExecutableMode.APPROVAL_WAITING_MODE)
        .build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    HarnessApprovalResponseData responseData = (HarnessApprovalResponseData) responseDataMap.values().iterator().next();
    HarnessApprovalInstance instance =
        (HarnessApprovalInstance) approvalInstanceService.get(responseData.getApprovalInstanceId());
    return StepResponse.builder()
        .status(instance.getStatus() == ApprovalStatus.APPROVED ? Status.SUCCEEDED : Status.FAILED)
        .stepOutcome(
            StepResponse.StepOutcome.builder().name("output").outcome(instance.toHarnessApprovalOutcome()).build())
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters, AsyncExecutableResponse executableResponse) {
    approvalInstanceService.expireByNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
  }
}
