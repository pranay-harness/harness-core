package io.harness.visitor.helpers.executionelement;

import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.yaml.core.StepElement;

public class StepElementVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement() {
    return StepElement.builder().build();
  }
}
