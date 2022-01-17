package io.harness.ci.plan.creator.step;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.plan.creator.CICreatorUtils;
import io.harness.filters.GenericStepPMSFilterJsonCreatorV2;

import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class CIStepFilterJsonCreatorV2 extends GenericStepPMSFilterJsonCreatorV2 {
  @Override
  public Set<String> getSupportedStepTypes() {
    return CICreatorUtils.getSupportedStepsV2();
  }
}
