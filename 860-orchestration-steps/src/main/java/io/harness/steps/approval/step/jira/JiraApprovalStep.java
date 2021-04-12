package io.harness.steps.approval.step.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableMode;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.repositories.ApprovalInstanceRepository;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.StepUtils;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.jira.beans.JiraApprovalResponseData;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;

@OwnedBy(CDC)
public class JiraApprovalStep implements AsyncExecutable<JiraApprovalStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(StepSpecTypeConstants.JIRA_APPROVAL).build();

  @Inject private ApprovalInstanceRepository approvalInstanceRepository;
  @Inject private ApprovalInstanceService approvalInstanceService;

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, JiraApprovalStepParameters stepParameters, StepInputPackage inputPackage) {
    ApprovalInstance approvalInstance = JiraApprovalInstance.fromStepParameters(ambiance, stepParameters);
    approvalInstanceRepository.save(approvalInstance);
    return AsyncExecutableResponse.newBuilder()
        .addCallbackIds(approvalInstance.getId())
        .setMode(AsyncExecutableMode.APPROVAL_WAITING_MODE)
        .addAllLogKeys(CollectionUtils.emptyIfNull(
            StepUtils.generateLogKeys(StepUtils.generateLogAbstractions(ambiance), Collections.emptyList())))
        .build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, JiraApprovalStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    JiraApprovalResponseData jiraApprovalResponseData =
        (JiraApprovalResponseData) responseDataMap.values().iterator().next();
    JiraApprovalInstance instance =
        (JiraApprovalInstance) approvalInstanceService.get(jiraApprovalResponseData.getInstanceId());
    return StepResponse.builder()
        .status(instance.getStatus() == ApprovalStatus.APPROVED ? Status.SUCCEEDED : Status.FAILED)
        .stepOutcome(
            StepResponse.StepOutcome.builder().name("output").outcome(instance.toJiraApprovalOutcome()).build())
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, JiraApprovalStepParameters stepParameters, AsyncExecutableResponse executableResponse) {
    approvalInstanceRepository.findByNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
        .ifPresent(instance -> approvalInstanceService.expire(instance.getId()));
  }

  @Override
  public Class<JiraApprovalStepParameters> getStepParametersClass() {
    return JiraApprovalStepParameters.class;
  }
}
