package io.harness.accesscontrol.roles.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.roles.Role;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, Role> {
  @Override
  public void initialize(ValidRole constraintAnnotation) {
    // nothing to initialize
  }

  @Override
  public boolean isValid(Role value, ConstraintValidatorContext context) {
    if (!value.isManaged() && value.getIdentifier().charAt(0) == '_') {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("custom roles cannot start with _").addConstraintViolation();
      return false;
    }
    if (value.isManaged() && value.getIdentifier().charAt(0) != '_') {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("managed roles should start with _").addConstraintViolation();
      return false;
    }
    if (!value.isManaged() && isEmpty(value.getScopeIdentifier())) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("custom roles cannot be created without a scope.")
          .addConstraintViolation();
      return false;
    }
    if (value.isManaged() && isNotEmpty(value.getScopeIdentifier())) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("managed roles should not have a scope.").addConstraintViolation();
      return false;
    }
    return true;
  }
}
