package io.harness.template.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDC)
public class TemplateInputsErrorResponseDTO {
  String errorYaml;
  Map<String, TemplateInputsErrorDTO> errorMap;
}
