package io.harness.ngpipeline.common;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
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
}
