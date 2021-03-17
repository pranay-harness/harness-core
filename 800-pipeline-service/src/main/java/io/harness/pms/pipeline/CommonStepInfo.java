package io.harness.pms.pipeline;

import io.harness.beans.FeatureName;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.helpers.PmsFeatureFlagHelper;

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
  public List<StepInfo> getCommonSteps(String accountId) {
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(shellScriptStepInfo);
    try {
      if (pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.NG_HARNESS_APPROVAL)) {
        stepInfos.add(harnessApprovalStepInfo);
      }
    } catch (Exception ex) {
      log.warn("Exception While checking Feature Flag. accountId: {} flag: {}", accountId,
          FeatureName.NG_HARNESS_APPROVAL, ex);
    }
    stepInfos.add(httpStepInfo);
    return stepInfos;
  }
}
