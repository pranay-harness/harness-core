package io.harness.pms.sdk;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.SdkStep;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.pipeline.CommonStepInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class SdkStepHelper {
  @Inject PmsSdkInstanceService pmsSdkInstanceService;
  @Inject CommonStepInfo commonStepInfo;

  public Set<String> getAllStepVisibleInUI() {
    Set<String> allSteps = new HashSet<>();
    for (Set<SdkStep> sdkSteps : pmsSdkInstanceService.getSdkSteps().values()) {
      allSteps.addAll(sdkSteps.stream()
                          .filter(SdkStep::getIsPartOfStepPallete)
                          .map(sdkStep -> sdkStep.getStepType().getType())
                          .collect(Collectors.toList()));
    }
    allSteps.addAll(commonStepInfo.getCommonSteps("").stream().map(StepInfo::getType).collect(Collectors.toList()));
    return allSteps;
  }
}
