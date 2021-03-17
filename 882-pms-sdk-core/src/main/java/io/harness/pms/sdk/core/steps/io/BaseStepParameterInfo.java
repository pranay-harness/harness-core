package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.yaml.ParameterField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BaseStepParameterInfo {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  RollbackInfo rollbackInfo;
  ParameterField<String> timeout;
}
