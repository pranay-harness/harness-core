package io.harness.ngpipeline.common;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public class ParameterFieldHelper {
  public <T> T getParameterFieldValue(ParameterField<T> fieldValue) {
    if (fieldValue == null) {
      return null;
    }
    return fieldValue.getValue();
  }
}
