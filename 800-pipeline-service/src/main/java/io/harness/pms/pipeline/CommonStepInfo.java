package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
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

@OwnedBy(PIPELINE)
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
  StepInfo harnessApprovalStepInfo = StepInfo.newBuilder()
                                         .setName("Harness Approval")
                                         .setType("HarnessApproval")
                                         .setStepMetaData(StepMetaData.newBuilder()
                                                              .addCategory("Provisioner")
                                                              .addCategory("Approval")
                                                              .setFolderPath("Approval")
                                                              .build())
                                         .setFeatureFlag(FeatureName.NG_HARNESS_APPROVAL.name())
                                         .build();
  StepInfo jiraApprovalStepInfo = StepInfo.newBuilder()
                                      .setName("Jira Approval")
                                      .setType("JiraApproval")
                                      .setStepMetaData(StepMetaData.newBuilder()
                                                           .addCategory("Provisioner")
                                                           .addCategory("Approval")
                                                           .setFolderPath("Approval")
                                                           .build())
                                      .setFeatureFlag(FeatureName.NG_HARNESS_APPROVAL.name())
                                      .build();
  StepInfo jiraCreateStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Create")
          .setType(StepSpecTypeConstants.JIRA_CREATE)
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Jira").setFolderPath("Jira").build())
          .setFeatureFlag(FeatureName.NG_HARNESS_APPROVAL.name())
          .build();
  StepInfo jiraUpdateStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Update")
          .setType(StepSpecTypeConstants.JIRA_UPDATE)
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Jira").setFolderPath("Jira").build())
          .setFeatureFlag(FeatureName.NG_HARNESS_APPROVAL.name())
          .build();
  StepInfo barrierStepInfo =
      StepInfo.newBuilder()
          .setName("Barrier")
          .setType("Barrier")
          .setStepMetaData(StepMetaData.newBuilder().setFolderPath("FlowControl/Barrier").build())
          .setFeatureFlag(FeatureName.NG_BARRIERS.name())
          .build();

  public List<StepInfo> getCommonSteps() {
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(shellScriptStepInfo);
    stepInfos.add(httpStepInfo);
    stepInfos.add(harnessApprovalStepInfo);
    stepInfos.add(jiraApprovalStepInfo);
    stepInfos.add(jiraCreateStepInfo);
    stepInfos.add(jiraUpdateStepInfo);
    stepInfos.add(barrierStepInfo);
    return stepInfos;
  }
}
