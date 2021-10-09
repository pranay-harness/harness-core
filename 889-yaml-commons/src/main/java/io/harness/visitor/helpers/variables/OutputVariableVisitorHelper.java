package io.harness.visitor.helpers.variables;

import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.yaml.core.variables.OutputNGVariable;

public class OutputVariableVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    OutputNGVariable element = (OutputNGVariable) originalElement;
    return OutputNGVariable.builder().name(element.getName()).build();
  }
}
