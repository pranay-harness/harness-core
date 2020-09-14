package io.harness.cdng.visitor.helpers.serviceconfig;

import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class KubernetesServiceSpecVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement() {
    return KubernetesServiceSpec.builder().build();
  }
}
