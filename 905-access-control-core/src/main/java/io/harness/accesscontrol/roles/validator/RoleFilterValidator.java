package io.harness.accesscontrol.roles.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.roles.filter.RoleFilter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RoleFilterValidator implements ConstraintValidator<ValidRoleFilter, RoleFilter> {
  @Override
  public void initialize(ValidRoleFilter constraintAnnotation) {
    // nothing to initialize
  }

  @Override
  public boolean isValid(RoleFilter value, ConstraintValidatorContext context) {
    if (isEmpty(value.getScopeIdentifier()) && value.isIncludeChildScopes()) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Invalid Role Filter: Cannot include child scopes when the scope filter is null.")
          .addConstraintViolation();
      return false;
    }
    if (value.isIncludeChildScopes() && !value.getManagedFilter().equals(ManagedFilter.ONLY_CUSTOM)) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Invalid Role Filter: Child scopes can only be included when the managed filter is only custom.")
          .addConstraintViolation();
      return false;
    }
    if (isEmpty(value.getScopeIdentifier()) && !value.getManagedFilter().equals(ManagedFilter.ONLY_MANAGED)) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Invalid Role Filter: Either managed filter should be set to only managed, or scope filter should be non-empty")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
