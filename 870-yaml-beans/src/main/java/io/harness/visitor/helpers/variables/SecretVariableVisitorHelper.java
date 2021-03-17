package io.harness.visitor.helpers.variables;

import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.SecretNGVariable;

public class SecretVariableVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    SecretNGVariable element = (SecretNGVariable) originalElement;
    return SecretNGVariable.builder().name(element.getName()).type(NGVariableType.SECRET).build();
  }
}