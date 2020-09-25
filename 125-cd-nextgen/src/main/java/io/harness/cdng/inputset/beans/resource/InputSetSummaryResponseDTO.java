package io.harness.cdng.inputset.beans.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.ngpipeline.InputSetEntityType;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("InputSetSummaryResponse")
public class InputSetSummaryResponseDTO {
  String identifier;
  String name;
  String pipelineIdentifier;
  String description;
  InputSetEntityType inputSetType;
  // add tags when needed
}