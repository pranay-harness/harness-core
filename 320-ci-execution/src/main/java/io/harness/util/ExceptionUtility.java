/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.util;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGExpressionUtils;
import io.harness.exception.ngexception.UnresolvedRunTimeInputSetException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
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
