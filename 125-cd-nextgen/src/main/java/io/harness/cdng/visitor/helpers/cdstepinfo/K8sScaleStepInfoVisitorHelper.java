package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.k8s.K8sScaleStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class K8sScaleStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    K8sScaleStepInfo k8sScaleStepInfo = (K8sScaleStepInfo) originalElement;
    return K8sScaleStepInfo.infoBuilder().identifier(k8sScaleStepInfo.getIdentifier()).build();
  }
}
