package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.steps.FolderPathConstants;
import io.harness.steps.StepCategoryConstants;
import io.harness.steps.StepSpecTypeConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class CommonStepInfo {
  private static final String APPROVAL_STEP_CATEGORY = "Approval";

  @Inject PmsFeatureFlagHelper pmsFeatureFlagHelper;

  StepInfo shellScriptStepInfo =
      StepInfo.newBuilder()
          .setName("Shell Script")
          .setType(StepSpecTypeConstants.SHELL_SCRIPT)
          .setStepMetaData(StepMetaData.newBuilder().addFolderPaths("Utilities/Scripted").build())
          .build();
  StepInfo httpStepInfo =
      StepInfo.newBuilder()
          .setName("Http")
          .setType("Http")
          .setStepMetaData(StepMetaData.newBuilder().addFolderPaths("Utilities/Non-Scripted").build())
          .build();
  StepInfo harnessApprovalStepInfo =
      StepInfo.newBuilder()
          .setName("Harness Approval")
          .setType("HarnessApproval")
          .setStepMetaData(StepMetaData.newBuilder()
                               .addCategory(StepCategoryConstants.PROVISIONER)
                               .addCategory(StepCategoryConstants.APPROVAL)
                               .addFolderPaths(FolderPathConstants.APPROVAL)
                               .build())
          .setFeatureRestrictionName(FeatureRestrictionName.INTEGRATED_APPROVALS_WITH_HARNESS_UI.name())
          .build();
  StepInfo jiraApprovalStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Approval")
          .setType("JiraApproval")
          .setStepMetaData(StepMetaData.newBuilder()
                               .addCategory(StepCategoryConstants.PROVISIONER)
                               .addCategory(StepCategoryConstants.APPROVAL)
                               .addFolderPaths(FolderPathConstants.APPROVAL)
                               .build())
          .setFeatureRestrictionName(FeatureRestrictionName.INTEGRATED_APPROVALS_WITH_JIRA.name())

          .build();
  StepInfo jiraCreateStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Create")
          .setType(StepSpecTypeConstants.JIRA_CREATE)
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Jira").addFolderPaths("Jira").build())
          .setFeatureRestrictionName(FeatureRestrictionName.INTEGRATED_APPROVALS_WITH_JIRA.name())

          .build();
  StepInfo jiraUpdateStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Update")
          .setType(StepSpecTypeConstants.JIRA_UPDATE)
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Jira").addFolderPaths("Jira").build())
          .setFeatureRestrictionName(FeatureRestrictionName.INTEGRATED_APPROVALS_WITH_JIRA.name())

          .build();
  StepInfo barrierStepInfo =
      StepInfo.newBuilder()
          .setName("Barrier")
          .setType("Barrier")
          .setStepMetaData(StepMetaData.newBuilder().addFolderPaths("FlowControl/Barrier").build())
          .build();
  StepInfo serviceNowApprovalStepInfo =
      StepInfo.newBuilder()
          .setName("ServiceNow Approval")
          .setType(StepSpecTypeConstants.SERVICENOW_APPROVAL)
          .setStepMetaData(StepMetaData.newBuilder()
                               .addCategory(StepCategoryConstants.PROVISIONER)
                               .addCategory(StepCategoryConstants.APPROVAL)
                               .addFolderPaths(FolderPathConstants.APPROVAL)
                               .build())
          .setFeatureRestrictionName(FeatureRestrictionName.INTEGRATED_APPROVALS_WITH_SERVICE_NOW.name())
          .setFeatureFlag(FeatureName.SERVICENOW_NG_INTEGRATION.name())
          .build();

  public List<StepInfo> getCommonSteps(String category) {
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(shellScriptStepInfo);
    stepInfos.add(httpStepInfo);
    stepInfos.add(harnessApprovalStepInfo);
    stepInfos.add(jiraApprovalStepInfo);
    stepInfos.add(jiraCreateStepInfo);
    stepInfos.add(jiraUpdateStepInfo);
    stepInfos.add(barrierStepInfo);
    stepInfos.add(serviceNowApprovalStepInfo);
    return stepInfos;
  }
}
