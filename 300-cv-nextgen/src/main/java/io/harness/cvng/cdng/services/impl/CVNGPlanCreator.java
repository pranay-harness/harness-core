package io.harness.cvng.cdng.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.cdng.beans.CVNGStepType;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;
@OwnedBy(HarnessTeam.CV)
public class CVNGPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CVNGStepType.CVNG_VERIFY.getDisplayName());
  }
}
