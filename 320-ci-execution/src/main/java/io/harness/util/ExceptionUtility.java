package io.harness.util;

import io.harness.common.NGExpressionUtils;
import io.harness.exception.ngexception.UnresolvedRunTimeInputSetException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionUtility {
  public void throwUnresolvedExpressionException(String expression, String field, String message) {
    if (NGExpressionUtils.matchesInputSetPattern(expression)) {
      throw new UnresolvedRunTimeInputSetException(String.format(
          "Failed to resolve runtime inputset: %s for mandatory field %s in " + message, expression, field));
    } else {
      throw new UnresolvedRunTimeInputSetException(
          String.format("Failed to resolve expression: %s for mandatory field %s in" + message, expression, field));
    }
  }
}
