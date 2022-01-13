/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
