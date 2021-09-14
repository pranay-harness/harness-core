/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.validation.annotations;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RequiredParameterFieldValidator implements ConstraintValidator<Required, Object> {
  @Override
  public void initialize(Required required) {
    // do Nothing
  }

  @Override
  public boolean isValid(Object s, ConstraintValidatorContext constraintValidatorContext) {
    // To be implemented with InputSet
    return s != null;
  }
}
