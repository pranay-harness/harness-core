package io.harness.cdng.creator.plan.steps;

import io.harness.plancreator.steps.GenericPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CDPMSStepPlanCreator extends GenericPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("K8sRollingDeploy", "K8sRollingRollback");
  }
}
