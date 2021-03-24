package io.harness.pms.pipeline;

import io.harness.beans.FeatureName;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.steps.StepSpecTypeConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CommonStepInfo {
  @Inject PmsFeatureFlagHelper pmsFeatureFlagHelper;

  StepInfo shellScriptStepInfo =
      StepInfo.newBuilder()
          .setName("Shell Script")
          .setType("ShellScript")
          .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Utilities/Scripted").build())
          .build();
  StepInfo httpStepInfo =
      StepInfo.newBuilder()
          .setName("Http")
          .setType("Http")
          .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Utilities/Non-Scripted").build())
          .build();
  StepInfo harnessApprovalStepInfo =
      StepInfo.newBuilder()
          .setName("Harness Approval")
          .setType("HarnessApproval")
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Approval").setFolderPath("Approval").build())
          .build();
  StepInfo jiraApprovalStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Approval")
          .setType("JiraApproval")
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Approval").setFolderPath("Approval").build())
          .build();
  StepInfo jiraCreateStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Create")
          .setType(StepSpecTypeConstants.JIRA_CREATE)
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Jira").setFolderPath("Jira").build())
          .build();
  StepInfo barrierStepInfo =
      StepInfo.newBuilder()
          .setName("Barrier")
          .setType("Barrier")
          .setStepMetaData(StepMetaData.newBuilder().setFolderPath("FlowControl/Barrier").build())
          .build();

  public List<StepInfo> getCommonSteps(String accountId) {
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(shellScriptStepInfo);
    stepInfos.add(httpStepInfo);
    addIfFeatureFlagEnabled(stepInfos, accountId);
    return stepInfos;
  }

  private void addIfFeatureFlagEnabled(List<StepInfo> stepInfos, String accountId) {
    String featureName = null;
    try {
      featureName = FeatureName.NG_HARNESS_APPROVAL.name();
      if (pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.NG_HARNESS_APPROVAL)) {
        stepInfos.add(harnessApprovalStepInfo);
        stepInfos.add(jiraApprovalStepInfo);
        stepInfos.add(jiraCreateStepInfo);
      }

      featureName = FeatureName.NG_BARRIERS.name();
      if (pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.NG_BARRIERS)) {
        stepInfos.add(barrierStepInfo);
      }
    } catch (Exception ex) {
      log.warn("Exception While checking Feature Flag. accountId: {} flag: {}", accountId, featureName, ex);
    }
  }
}
