/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
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
  private static final String APPROVAL_STEP_CATEGORY = "Approval";

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
                                         .build();
  StepInfo jiraApprovalStepInfo = StepInfo.newBuilder()
                                      .setName("Jira Approval")
                                      .setType("JiraApproval")
                                      .setStepMetaData(StepMetaData.newBuilder()
                                                           .addCategory("Provisioner")
                                                           .addCategory("Approval")
                                                           .setFolderPath("Approval")
                                                           .build())
                                      .build();
  StepInfo jiraCreateStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Create")
          .setType(StepSpecTypeConstants.JIRA_CREATE)
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Jira").setFolderPath("Jira").build())
          .build();
  StepInfo jiraUpdateStepInfo =
      StepInfo.newBuilder()
          .setName("Jira Update")
          .setType(StepSpecTypeConstants.JIRA_UPDATE)
          .setStepMetaData(StepMetaData.newBuilder().addCategory("Jira").setFolderPath("Jira").build())
          .build();
  StepInfo barrierStepInfo =
      StepInfo.newBuilder()
          .setName("Barrier")
          .setType("Barrier")
          .setStepMetaData(StepMetaData.newBuilder().setFolderPath("FlowControl/Barrier").build())
          .build();

  public List<StepInfo> getCommonSteps(String category) {
    List<StepInfo> stepInfos = new ArrayList<>();
    // Remove shell script from approval stage till shell script step is moved to pipeline service.
    if (!APPROVAL_STEP_CATEGORY.equals(category)) {
      stepInfos.add(shellScriptStepInfo);
    }
    stepInfos.add(httpStepInfo);
    stepInfos.add(harnessApprovalStepInfo);
    stepInfos.add(jiraApprovalStepInfo);
    stepInfos.add(jiraCreateStepInfo);
    stepInfos.add(jiraUpdateStepInfo);
    stepInfos.add(barrierStepInfo);
    return stepInfos;
  }
}
