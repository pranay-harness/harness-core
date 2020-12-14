package io.harness.pms.pipeline;

import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonStepInfo {
  StepInfo shellScriptStepInfo =
      StepInfo.newBuilder()
          .setName("Shell Script")
          .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Utilities/Scripted").build())
          .build();
  StepInfo httpStepInfo =
      StepInfo.newBuilder()
          .setName("Http")
          .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Utilities/Non-Scripted").build())
          .build();

  public List<StepInfo> getCommonSteps() {
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(shellScriptStepInfo);
    stepInfos.add(httpStepInfo);
    return stepInfos;
  }
}
