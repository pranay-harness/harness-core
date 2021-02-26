package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.k8s.K8sCanaryStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class K8sCanaryStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    K8sCanaryStepInfo k8sCanaryStepInfo = (K8sCanaryStepInfo) originalElement;
    return K8sCanaryStepInfo.infoBuilder().identifier(k8sCanaryStepInfo.getIdentifier()).build();
  }
}
