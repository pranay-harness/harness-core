package io.harness.cdng.visitor.helpers.deploymentstage;

import io.harness.cdng.variables.StageVariables;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class StageVariablesVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // To be added in next iteration of validation framework.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return StageVariables.builder().build();
  }
}
