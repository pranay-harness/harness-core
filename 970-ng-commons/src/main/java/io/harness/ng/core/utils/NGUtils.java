/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import io.dropwizard.jersey.validation.JerseyViolationException;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
@OwnedBy(PL)
public class NGUtils {
  private static final Validator validator = Validation.buildDefaultValidatorFactory().usingContext().getValidator();

  private static String getValuesMisMatchErrorMessage(Pair<?, ?> pair) {
    return "Value mismatch, previous: " + pair.getKey() + " current: " + pair.getValue();
  }

  public static void verifyValuesNotChanged(List<Pair<?, ?>> valuesList) {
    for (Pair<?, ?> pair : valuesList) {
      if (!pair.getKey().equals(pair.getValue())) {
        throw new InvalidRequestException(getValuesMisMatchErrorMessage(pair));
      }
    }
  }

  public static void validate(Object entity) {
    Set<ConstraintViolation<Object>> constraints = validator.validate(entity);
    if (isNotEmpty(constraints)) {
      throw new JerseyViolationException(constraints, null);
    }
  }

  public static void verifyValuesNotChanged(List<Pair<?, ?>> valuesList, boolean present) {
    if (present) {
      for (Pair<?, ?> pair : valuesList) {
        if (pair.getValue() != null && (pair.getKey() == null || !pair.getKey().equals(pair.getValue()))) {
          throw new InvalidRequestException(getValuesMisMatchErrorMessage(pair));
        }
      }
    } else {
      for (Pair<?, ?> pair : valuesList) {
        if (!pair.getKey().equals(pair.getValue())) {
          throw new InvalidRequestException(getValuesMisMatchErrorMessage(pair));
        }
      }
    }
  }
}
