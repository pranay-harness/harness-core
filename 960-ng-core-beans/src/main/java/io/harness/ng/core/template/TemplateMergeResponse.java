package io.harness.ng.core.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class TemplateMergeResponse {
  String mergedPipelineYaml;
  boolean isValid;
  TemplateInputsErrorResponseDTO errorResponse;
  List<TemplateReferenceSummary> templateReferenceSummaries;
}
