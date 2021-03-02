package io.harness.plancreator.steps.internal;

import io.harness.plancreator.steps.GenericStepPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class BarrierPMSPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("Barrier");
  }
}
