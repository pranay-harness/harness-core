/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.validator;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class NGEntityNameValidator implements ConstraintValidator<NGEntityName, String> {
  private static final String ALLOWED_CHARS_STRING =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ ";
  private static final int MAX_ALLOWED_LENGTH = 64;

  @Override
  public void initialize(NGEntityName constraintAnnotation) {
    // nothing to initialize
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (!StringUtils.isNotBlank(value)) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("cannot be empty.").addConstraintViolation();
      return false;
    }
    if (value.length() > MAX_ALLOWED_LENGTH) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("cannot be more than 64 characters long.").addConstraintViolation();
      return false;
    }
    if (!Sets.newHashSet(Lists.charactersOf(ALLOWED_CHARS_STRING)).containsAll(Lists.charactersOf(value))) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("can only contain alphanumeric, hyphen and underscore characters.")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
