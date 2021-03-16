package io.harness.plancreator.steps.approval;

import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class ApproverInputInfo {
  @NotEmpty String name;
  ParameterField<String> defaultValue;
}
