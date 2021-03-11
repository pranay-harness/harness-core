package io.harness.plancreator.steps.approval;

import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApproverInputInfo {
  String name;
  ParameterField<String> defaultValue;
}
