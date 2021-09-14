/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.execution.utils;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SkipInfoUtils {
  public String getSkipCondition(ParameterField<String> skipCondition) {
    if (skipCondition == null) {
      return null;
    }
    if (EmptyPredicate.isNotEmpty(skipCondition.getValue())) {
      return skipCondition.getValue();
    }
    return skipCondition.getExpressionValue();
  }
}
