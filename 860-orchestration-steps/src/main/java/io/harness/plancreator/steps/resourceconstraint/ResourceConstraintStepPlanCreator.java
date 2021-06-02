package io.harness.plancreator.steps.resourceconstraint;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.internal.PMSStepPlanCreator;
import io.harness.pms.yaml.YamlField;
import io.harness.steps.StepSpecTypeConstants;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class ResourceConstraintStepPlanCreator extends PMSStepPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.RESOURCE_CONSTRAINT);
  }

  @Override
  protected YamlField obtainNextSiblingField(YamlField currentField) {
    return currentField.getNode().nextSiblingNodeFromParentObject("execution");
  }
}
