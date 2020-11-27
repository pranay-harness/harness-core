package io.harness.visitor.helpers.variables;

import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.StringNGVariable;

public class StringVariableVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    StringNGVariable element = (StringNGVariable) originalElement;
    return StringNGVariable.builder().name(element.getName()).type(NGVariableType.STRING).build();
  }
}
