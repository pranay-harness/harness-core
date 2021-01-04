package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.k8s.K8sBlueGreenStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class K8sBlueGreenStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    K8sBlueGreenStepInfo k8sBlueGreenStepInfo = (K8sBlueGreenStepInfo) originalElement;
    return K8sBlueGreenStepInfo.infoBuilder().identifier(k8sBlueGreenStepInfo.getIdentifier()).build();
  }
}