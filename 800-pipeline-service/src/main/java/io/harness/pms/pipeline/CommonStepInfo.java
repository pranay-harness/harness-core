package io.harness.pms.pipeline;

import io.harness.beans.FeatureName;
import io.harness.ng.core.account.remote.AccountClient;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.remote.client.RestClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CommonStepInfo {
  @Inject AccountClient accountClient;

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
  public List<StepInfo> getCommonSteps(String accountId) {
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(shellScriptStepInfo);
    try {
      if (RestClientUtils.getResponse(
              accountClient.isFeatureFlagEnabled(FeatureName.NG_HARNESS_APPROVAL.name(), accountId))) {
        stepInfos.add(harnessApprovalStepInfo);
      }
    } catch (Exception ex) {
      log.warn("Exception While checking Feature Flag. accountId: {} flag: {}", accountId,
          FeatureName.NG_HARNESS_APPROVAL, ex);
    }
    stepInfos.add(httpStepInfo);
    stepInfos.add(jiraApprovalStepInfo);
    return stepInfos;
  }
}
