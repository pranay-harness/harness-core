/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.roles.Role;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@OwnedBy(HarnessTeam.PL)
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
