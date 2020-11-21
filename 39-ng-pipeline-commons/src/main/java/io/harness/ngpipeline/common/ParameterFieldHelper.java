package io.harness.ngpipeline.common;

import io.harness.beans.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ParameterFieldHelper {
  public <T> T getParameterFieldValue(ParameterField<T> fieldValue) {
    if (fieldValue == null) {
      return null;
    }
    return fieldValue.getValue();
  }
}
