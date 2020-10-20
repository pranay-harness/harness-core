package io.harness.pipeline.plan.scratch.cd;

import com.google.common.collect.Sets;

import io.harness.pipeline.plan.scratch.lib.creator.SimpleStepPlanCreator;

import java.util.Set;

public class CDStepPlanCreator extends SimpleStepPlanCreator {
  private static final Set<String> supportedStepTypes = Sets.newHashSet("k8sCanary", "k8sRolling");

  @Override
  public Set<String> getSupportedStepTypes() {
    return supportedStepTypes;
  }
}
