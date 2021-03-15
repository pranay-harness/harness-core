package io.harness.plancreator.steps.approval;

import io.harness.plancreator.steps.GenericStepPMSPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class ApprovalPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("HarnessApproval", "JiraApproval");
  }
}
