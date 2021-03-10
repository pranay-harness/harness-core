package io.harness.accesscontrol.roleassignments.validator;

import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RoleAssignmentFilterValidator
    implements ConstraintValidator<ValidRoleAssignmentFilter, RoleAssignmentFilter> {
  @Override
  public void initialize(ValidRoleAssignmentFilter constraintAnnotation) {
    // nothing to initialize
  }

  @Override
  public boolean isValid(RoleAssignmentFilter value, ConstraintValidatorContext context) {
    if (!value.getPrincipalFilter().isEmpty() && !value.getPrincipalTypeFilter().isEmpty()) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "RoleAssignment Filter can have either a principal filter or a principal type filter.")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
