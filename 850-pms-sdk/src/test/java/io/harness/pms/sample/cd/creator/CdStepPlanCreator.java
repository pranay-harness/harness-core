package io.harness.pms.sample.cd.creator;

import io.harness.pms.plan.creator.plan.SimpleStepPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CdStepPlanCreator extends SimpleStepPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("k8sCanary", "k8sRolling");
  }
}
