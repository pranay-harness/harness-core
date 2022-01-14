package io.harness.cdng.visitor.helpers.serviceconfig;

import io.harness.cdng.service.beans.NativeHelmServiceSpec;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class NativeHelmServiceSpecVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // nothing to validate
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return NativeHelmServiceSpec.builder().build();
  }
}
