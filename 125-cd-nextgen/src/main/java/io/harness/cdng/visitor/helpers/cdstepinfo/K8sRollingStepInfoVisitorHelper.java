package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class K8sRollingStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    K8sRollingRollbackStepInfo k8sRollingRollbackStepInfo = (K8sRollingRollbackStepInfo) originalElement;
    return K8sRollingStepInfo.infoBuilder().identifier(k8sRollingRollbackStepInfo.getIdentifier()).build();
  }
}
