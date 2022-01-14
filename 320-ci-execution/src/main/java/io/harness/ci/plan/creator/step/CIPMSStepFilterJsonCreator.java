package io.harness.ci.plan.creator.step;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.plan.creator.CICreatorUtils;
import io.harness.filters.GenericStepPMSFilterJsonCreator;

import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class CIPMSStepFilterJsonCreator extends GenericStepPMSFilterJsonCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return CICreatorUtils.getSupportedSteps();
  }
}
