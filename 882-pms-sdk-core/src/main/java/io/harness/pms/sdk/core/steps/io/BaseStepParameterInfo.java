package io.harness.pms.sdk.core.steps.io;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@OwnedBy(PIPELINE)
public class BaseStepParameterInfo {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  ParameterField<String> when;
  ParameterField<String> timeout;
}
