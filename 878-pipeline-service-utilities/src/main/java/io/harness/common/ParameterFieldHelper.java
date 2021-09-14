/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class ParameterFieldHelper {
  public <T> T getParameterFieldValue(ParameterField<T> fieldValue) {
    if (fieldValue == null) {
      return null;
    }
    return fieldValue.getValue();
  }

  public Boolean getBooleanParameterFieldValue(ParameterField<?> fieldValue) {
    Object value = getParameterFieldValue(fieldValue);

    if (value == null) {
      return Boolean.FALSE;
    }

    if (value instanceof String) {
      String valueString = (String) value;
      if (!valueString.equalsIgnoreCase("true") && !valueString.equalsIgnoreCase("false")) {
        throw new IllegalArgumentException(String.format("Expected 'true' or 'false' value, got %s", valueString));
      }

      return Boolean.valueOf((String) value);
    }

    return (Boolean) value;
  }

  public String getParameterFieldValueHandleValueNull(ParameterField<String> fieldValue) {
    if (isNull(fieldValue)) {
      return null;
    }

    return isNull(fieldValue.getValue()) ? "" : fieldValue.getValue();
  }

  public ParameterField<String> getParameterFieldHandleValueNull(ParameterField<String> fieldValue) {
    if (isNull(fieldValue)) {
      return null;
    }

    if (isNull(fieldValue.getValue())) {
      fieldValue.setValue("");
    }

    return fieldValue;
  }
}
