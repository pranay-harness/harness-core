package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class K8sRollingRollbackStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement() {
    return K8sRollingRollbackStepInfo.infoBuilder().build();
  }
}
