package io.harness.visitor.helpers.variables;

import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.NumberNGVariable;

public class NumberVariableVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    NumberNGVariable element = (NumberNGVariable) originalElement;
    return NumberNGVariable.builder().name(element.getName()).type(NGVariableType.NUMBER).build();
  }
}
