package io.harness.pms.sample.cd.creator;

import com.google.common.collect.Sets;

import io.harness.pms.sdk.creator.SimpleStepPlanCreator;

import java.util.Set;

public class CdStepPlanCreator extends SimpleStepPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("k8sCanary", "k8sRolling");
  }
}
