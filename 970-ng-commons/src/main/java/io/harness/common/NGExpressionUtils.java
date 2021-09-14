/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.expression.EngineExpressionEvaluator;

import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGExpressionUtils {
  private static final Pattern InputSetVariablePattern = Pattern.compile(
      EngineExpressionEvaluator.EXPR_START_ESC + "input" + EngineExpressionEvaluator.EXPR_END_ESC + ".*");
  public static final String DEFAULT_INPUT_SET_EXPRESSION =
      EngineExpressionEvaluator.EXPR_START + "input" + EngineExpressionEvaluator.EXPR_END;

  public boolean matchesInputSetPattern(String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return NGExpressionUtils.InputSetVariablePattern.matcher(expression).matches();
  }

  public boolean matchesPattern(Pattern pattern, String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return pattern.matcher(expression).matches();
  }

  // Function which matches pattern on given expression.
  public boolean containsPattern(Pattern pattern, String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return pattern.matcher(expression).find();
  }

  public String getInputSetValidatorPattern(String validatorName) {
    return "\\." + validatorName + "\\(";
  }

  public boolean isRuntimeOrExpressionField(String fieldValue) {
    return matchesInputSetPattern(fieldValue) || EngineExpressionEvaluator.hasExpressions(fieldValue);
  }
}
