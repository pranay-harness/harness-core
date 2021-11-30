package io.harness.cvng.core.validators;

import io.harness.beans.WithIdentifier;

import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UniqueIdentifierValidator
    implements ConstraintValidator<UniqueIdentifierCheck, List<? extends WithIdentifier>> {
  @Override
  public void initialize(UniqueIdentifierCheck listUniqueIdentifier) {}

  @Override
  public boolean isValid(
      List<? extends WithIdentifier> withIdentifiers, ConstraintValidatorContext constraintValidatorContext) {
    return withIdentifiers.size()
        == withIdentifiers.stream().map(withIdentifier -> withIdentifier.getIdentifier()).distinct().count();
  }
}
