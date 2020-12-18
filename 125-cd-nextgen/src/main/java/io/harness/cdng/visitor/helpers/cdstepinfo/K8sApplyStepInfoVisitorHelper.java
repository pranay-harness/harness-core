package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.k8s.K8sApplyStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class K8sApplyStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    K8sApplyStepInfo k8sApplyStepInfo = (K8sApplyStepInfo) originalElement;
    return K8sApplyStepInfo.infoBuilder().identifier(k8sApplyStepInfo.getIdentifier()).build();
  }
}
