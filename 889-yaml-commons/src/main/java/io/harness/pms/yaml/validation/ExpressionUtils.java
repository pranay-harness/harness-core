/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.yaml.validation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExpressionUtils {
  public boolean matchesPattern(Pattern pattern, String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return pattern.matcher(expression).matches();
  }

  public boolean containsPattern(Pattern pattern, String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return pattern.matcher(expression).find();
  }
}
