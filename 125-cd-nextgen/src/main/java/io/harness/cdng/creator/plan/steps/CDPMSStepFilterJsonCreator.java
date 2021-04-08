package io.harness.cdng.creator.plan.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.CDCreatorUtils;
import io.harness.filters.GenericStepPMSFilterJsonCreator;

import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class CDPMSStepFilterJsonCreator extends GenericStepPMSFilterJsonCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return CDCreatorUtils.getSupportedSteps();
  }
}
