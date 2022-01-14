package io.harness.ng.core.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("TemplateMergeResponse")
@Schema(name = "TemplateMergeResponse",
    description = "This is the view of the TemplateMergeResponse entity defined in Harness")
public class TemplateMergeResponseDTO {
  String mergedPipelineYaml;
  // Only TemplateReferences which you are directly using in your given yaml. Suppose you are referencing stage template
  // which has step template ref, then it returns only stage template.
  List<TemplateReferenceSummary> templateReferenceSummaries;
}
