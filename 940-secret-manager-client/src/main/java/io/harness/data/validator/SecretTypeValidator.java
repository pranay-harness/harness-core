/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.data.validator;

import io.harness.secretmanagerclient.SecretType;

import java.util.Arrays;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

public class SecretTypeValidator implements ConstraintValidator<SecretTypeAllowedValues, SecretType> {
  @Override
  public void initialize(SecretTypeAllowedValues constraintAnnotation) {
    // not required
  }

  @Override
  public boolean isValid(SecretType inputSecretType, ConstraintValidatorContext context) {
    SecretType[] secretTypesAllowed = (SecretType[]) ((ConstraintValidatorContextImpl) context)
                                          .getConstraintDescriptor()
                                          .getAttributes()
                                          .get("allowedValues");
    return Arrays.stream(secretTypesAllowed).anyMatch(allowedValue -> allowedValue == inputSecretType);
  }
}
