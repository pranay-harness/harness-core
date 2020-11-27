package io.harness.walktree.visitor.validation.annotations;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RequiredStringValidator implements ConstraintValidator<Required, String> {
  @Override
  public void initialize(Required required) {
    // do Nothing
  }

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    // To be implemented with InputSet
    return s != null;
  }
}
