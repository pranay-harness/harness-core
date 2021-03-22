package io.harness.cdng.creator.plan.steps;

import io.harness.plancreator.steps.GenericStepPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CDPMSStepPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("K8sRollingDeploy", "K8sRollingRollback", "K8sScale", "K8sCanaryDeploy",
        "K8sBlueGreenDeploy", "K8sBGSwapServices", "K8sDelete", "K8sCanaryDelete", "K8sApply");
  }
}
