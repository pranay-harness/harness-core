package io.harness.template.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class TemplateMergeResponse {
  String mergedPipelineYaml;
  boolean isValid;
  TemplateInputsErrorResponseDTO errorResponse;
}
