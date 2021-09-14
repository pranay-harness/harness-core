/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.validator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@OwnedBy(PL)
public class EntityIdentifierValidator implements ConstraintValidator<EntityIdentifier, String> {
  // Max Length : 64 characters
  // Start with: Alphabets, characters or Underscore
  // Chars Allowed : Alphanumeric, Underscore, $
  public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][0-9a-zA-Z_$]{0,63}$");
  public static final Set<String> NOT_ALLOWED_WORDS =
      Stream
          .of("or", "and", "eq", "ne", "lt", "gt", "le", "ge", "div", "mod", "not", "null", "true", "false", "new",
              "var", "return")
          .collect(Collectors.toCollection(HashSet::new));
  private boolean allowBlank;

  @Override
  public void initialize(EntityIdentifier constraintAnnotation) {
    // Nothing to initialize
    allowBlank = constraintAnnotation.allowBlank();
  }

  @Override
  public boolean isValid(String identifier, ConstraintValidatorContext context) {
    if (allowBlank && isBlank(identifier)) {
      return true;
    }
    if (!allowBlank && isBlank(identifier)) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("cannot be empty").addConstraintViolation();
      return false;
    }
    if (!matchesIdentifierPattern(identifier)) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "can be 64 characters long and can only contain alphanumeric, underscore and $ characters,"
              + " and not start with a number")
          .addConstraintViolation();
      return false;
    }
    if (!hasAllowedWords(identifier)) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(identifier + "is a keyword, so cannot be used")
          .addConstraintViolation();
      return false;
    }
    return true;
  }

  @VisibleForTesting
  boolean matchesIdentifierPattern(String identifier) {
    return IDENTIFIER_PATTERN.matcher(identifier).matches();
  }

  @VisibleForTesting
  boolean hasAllowedWords(String identifier) {
    return identifier != null && !NOT_ALLOWED_WORDS.contains(identifier);
  }
}
