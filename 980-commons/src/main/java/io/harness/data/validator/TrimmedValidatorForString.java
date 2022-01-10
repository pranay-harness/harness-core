/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.data.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TrimmedValidatorForString implements ConstraintValidator<Trimmed, String> {
  @Override
  public void initialize(Trimmed constraintAnnotation) {
    // Nothing to initialize
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (isEmpty(value)) {
      return true;
    }

    return !Character.isWhitespace(value.charAt(0)) && !Character.isWhitespace(value.charAt(value.length() - 1));
  }
}
