package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.helm.HelmDeployStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class HelmDeployStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return HelmDeployStepInfo.builder().build();
  }

  @Override
  public void validate(Object object, ValidationVisitor visitor) {}
}
